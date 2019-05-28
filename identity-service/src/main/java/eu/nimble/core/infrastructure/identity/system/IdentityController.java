package eu.nimble.core.infrastructure.identity.system;

import com.auth0.jwt.JWT;
import eu.nimble.core.infrastructure.identity.clients.IndexingClient;
import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.system.dto.CompanyRegistrationResponse;
import eu.nimble.core.infrastructure.identity.system.dto.UserRegistration;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.core.infrastructure.identity.utils.DataModelUtils;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class IdentityController {

    public static final String REFRESH_TOKEN_SESSION_KEY = "refresh_token";

    private static final Logger logger = LoggerFactory.getLogger(IdentityController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private QualifyingPartyRepository qualifyingPartyRepository;

    @Autowired
    private DeliveryTermsRepository deliveryTermsRepository;

    @Autowired
    private PaymentMeansRepository paymentMeansRepository;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @Autowired
    private OAuthClient oAuthClient;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private UblUtils ublUtils;

    @Autowired
    private IndexingClient indexingClient;

    @ApiOperation(value = "Authenticate a federated user.", response = FrontEndUser.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User authenticated", response = FrontEndUser.class),
            @ApiResponse(code = 400, message = "Invalid Token", response = FrontEndUser.class)})
    @RequestMapping(value = "/federation/login", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> loginFederatedUser(
            @ApiParam(value = "Access token provided by the IDP", required = true) @RequestBody Token token) {

        FrontEndUser frontEndUser = new FrontEndUser();
        String audience = JWT.decode(token.getAccessToken()).getClaim("aud").asString();
        String keycloakUserID = JWT.decode(token.getAccessToken()).getClaim("sub").asString();
        String email = JWT.decode(token.getAccessToken()).getClaim("email").asString();

        // check identity database
        UaaUser potentialUser = uaaUserRepository.findByExternalID(keycloakUserID);
        if (potentialUser == null) {
            logger.info("User " + email + " not found in local database, but on Keycloak.");
            // create a new user

            frontEndUser.setUsername(email);
            frontEndUser.setFirstname(JWT.decode(token.getAccessToken()).getClaim("name").asString());
            frontEndUser.setLastname(JWT.decode(token.getAccessToken()).getClaim("family_name").asString());
            // create UBL party of user
            PersonType newUser = UblAdapter.adaptPerson(frontEndUser);
            personRepository.save(newUser);

            // update id of user
            newUser.setID(newUser.getHjid().toString());
            personRepository.save(newUser);

            // create entry in identity DB
            UaaUser uaaUser = new UaaUser(email, newUser, keycloakUserID);
            uaaUserRepository.save(uaaUser);

            // update user data
            frontEndUser.setUserID(Long.parseLong(newUser.getID()));
            frontEndUser.setUsername(email);
            frontEndUser.setAccessToken(token.getAccessToken());
            httpSession.setAttribute(REFRESH_TOKEN_SESSION_KEY, token.getAccessToken());
            logger.info("Registering a new user with email {} and id {}", frontEndUser.getEmail(), frontEndUser.getUserID());

        }else {
            // create front end user DTO
            List<PartyType> companies = partyRepository.findByPerson(potentialUser.getUBLPerson());
            frontEndUser = UblAdapter.adaptUser(potentialUser, companies);

            // set and store tokens
            frontEndUser.setAccessToken(token.getAccessToken());
            httpSession.setAttribute(REFRESH_TOKEN_SESSION_KEY, token.getAccessToken());
            logger.info("User " + email + " successfully logged in.");
        }

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }

    @ApiOperation(value = "Register a new user to the nimble.", response = FrontEndUser.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User created", response = FrontEndUser.class),
            @ApiResponse(code = 405, message = "User not registered", response = FrontEndUser.class)})
    @RequestMapping(value = "/register/user", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> registerUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody UserRegistration userRegistration) {

        FrontEndUser frontEndUser = userRegistration.getUser();
        Credentials credentials = userRegistration.getCredentials();

        // validate data
        if (frontEndUser == null || credentials == null
                || credentials.getUsername() == null || credentials.getUsername().equals(frontEndUser.getEmail()) == false) {
            logger.info(" Tried to register an invalid user {}", userRegistration.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // create user on Keycloak
        String keycloakID;
        try {
            keycloakID = keycloakAdmin.registerUser(
                    frontEndUser.getFirstname(), frontEndUser.getLastname(), credentials.getPassword(), frontEndUser.getEmail());
        } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() == HttpStatus.CONFLICT.value())
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            throw ex;
        }

        // create UBL party of user
        PersonType newUser = UblAdapter.adaptPerson(frontEndUser);
        personRepository.save(newUser);

        // update id of user
        newUser.setID(newUser.getHjid().toString());
        personRepository.save(newUser);

        // create entry in identity DB
        UaaUser uaaUser = new UaaUser(credentials.getUsername(), newUser, keycloakID);
        uaaUserRepository.save(uaaUser);

        // update user data
        frontEndUser.setUserID(Long.parseLong(newUser.getID()));
        frontEndUser.setUsername(credentials.getUsername());

        // check if user was invited and add to company
        Optional<UserInvitation> invitationOpt = userInvitationRepository.findByEmail(frontEndUser.getEmail()).stream().findFirst();
        if (invitationOpt.isPresent()) {
            UserInvitation invitation = invitationOpt.get();

            // fetch company
            Optional<PartyType> companyOpt = partyRepository.findByHjid(Long.parseLong(invitation.getCompanyId())).stream().findFirst();
            if (companyOpt.isPresent() == false) {
                logger.error("Invalid invitation: Company %s not found", invitation.getCompanyId());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            PartyType company = companyOpt.get();

            // add new user
            company.getPerson().add(newUser);
            partyRepository.save(company);

            // save new state of invitation
            invitation.setPending(false);
            userInvitationRepository.save(invitation);

            // set roles
            for (String role : invitation.getRoleIDs()) {
                try {
                    keycloakAdmin.addRole(keycloakID, role);
                }
                catch (Exception ex) {
                    logger.error("Error while setting role", ex);
                }
            }

            logger.info("Invitation: added user {}({}) to company {}({})", frontEndUser.getEmail(), newUser.getID(), ublUtils.getName(company), UblAdapter.adaptPartyIdentifier(company));
        }

        logger.info("Registering a new user with email {} and id {}", frontEndUser.getEmail(), frontEndUser.getUserID());

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }

    public @ApiOperation(value = "", notes = "Register company controller.", response = CompanyRegistration.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Company registered", response = CompanyRegistration.class),
            @ApiResponse(code = 405, message = "Company not registered", response = CompanyRegistration.class)})
    @RequestMapping(value = "/register/company", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> registerCompany(
            @ApiParam(value = "Company object that is registered on NIMBLE.", required = true) @RequestBody CompanyRegistration companyRegistration,
            @RequestHeader(value = "Authorization") String bearer,
            HttpServletResponse response, HttpServletRequest request) {

        Address companyAddress = companyRegistration.getSettings().getDetails().getAddress();
        if (companyAddress == null || companyRegistration.getSettings().getDetails().getLegalName() == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Optional<PersonType> userPartyOpt = personRepository.findByHjid(companyRegistration.getUserID()).stream().findFirst();

        // check if user exists
        if (userPartyOpt.isPresent() == false) {
            logger.info("Cannot find user with id {}", companyRegistration.getUserID());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        PersonType userParty = userPartyOpt.get();

        // create company
        PartyType newCompany = UblAdapter.adaptCompanyRegistration(companyRegistration, userParty);
        partyRepository.save(newCompany);

        // create delivery terms
        DeliveryTermsType blankDeliveryTerms = new DeliveryTermsType();
        deliveryTermsRepository.save(blankDeliveryTerms);
        blankDeliveryTerms.setID(blankDeliveryTerms.getHjid().toString());
        deliveryTermsRepository.save(blankDeliveryTerms);

        // create payment means
        PaymentMeansType paymentMeans = UblUtils.emptyUBLObject(new PaymentMeansType());
        paymentMeansRepository.save(paymentMeans);
        paymentMeans.setID(paymentMeans.getHjid().toString());
        paymentMeansRepository.save(paymentMeans);

        // create purchase terms
        TradingPreferences purchaseTerms = new TradingPreferences();
        purchaseTerms.getDeliveryTerms().clear();
        purchaseTerms.getDeliveryTerms().add(blankDeliveryTerms);
        purchaseTerms.getPaymentMeans().clear();
        purchaseTerms.getPaymentMeans().add(paymentMeans);
        newCompany.setPurchaseTerms(purchaseTerms);

        // update id of company
        UblUtils.setID(newCompany, newCompany.getHjid().toString());
        partyRepository.save(newCompany);

        // create qualifying party
        QualifyingPartyType qualifyingParty = UblAdapter.adaptQualifyingParty(companyRegistration.getSettings(), newCompany);
        qualifyingPartyRepository.save(qualifyingParty);

        // add id to response object
        companyRegistration.setCompanyID(newCompany.getHjid());
        companyRegistration.setUserID(userParty.getHjid());

        // adapt role of user and refresh access token
        try {
            String keyCloakId = getKeycloakUserId(userParty);
            keycloakAdmin.addRole(keyCloakId, KeycloakAdmin.INITIAL_REPRESENTATIVE_ROLE);
        } catch (Exception e) {
            logger.error("Could not set role for user " + userParty.getID(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // inform platform managers
        try {
            informPlatformManager(userParty, newCompany);
        } catch (Exception ex) {
            logger.error("Could not notify platform managers", ex);
        }

        // refresh tokens
        if( httpSession.getAttribute(REFRESH_TOKEN_SESSION_KEY) != null) {
            OAuth2AccessToken tokenResponse = oAuthClient.refreshToken(httpSession.getAttribute(REFRESH_TOKEN_SESSION_KEY).toString());
            httpSession.setAttribute(REFRESH_TOKEN_SESSION_KEY, tokenResponse.getRefreshToken());
            companyRegistration.setAccessToken(tokenResponse.getValue());
        }

        //indexing the new company in the indexing service
        eu.nimble.service.model.solr.party.PartyType newParty = DataModelUtils.toIndexParty(newCompany);
        Map<NimbleConfigurationProperties.LanguageID, String> businessKeywords = companyRegistration.getSettings().getDetails().getBusinessKeywords();
        List<TextType> keywordsList = UblAdapter.adaptLanguageMapToTextType(businessKeywords);
        for(TextType keyword : keywordsList){
            newParty.addBusinessKeyword(keyword.getLanguageID(), keyword.getValue());
        }
        indexingClient.setParty(newParty);

        logger.info("Registered company with id {} for user with id {}", companyRegistration.getCompanyID(), companyRegistration.getUserID());

        return new ResponseEntity<>(companyRegistration, HttpStatus.OK);
    }

    @ApiOperation(value = "", notes = "Login controller with credentials.", response = CompanyRegistrationResponse.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful login", response = FrontEndUser.class),
            @ApiResponse(code = 401, message = "Login failed", response = FrontEndUser.class)})
    @RequestMapping(value = "/login", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> loginUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody Credentials credentials,
            HttpServletResponse response) {

        // invalidate old session (e.g. of previous user)
        httpSession.invalidate();

        // try to obtain access token
        OAuth2AccessToken accessToken;
        String keycloakUserID;
        try {
            logger.info("User " + credentials.getUsername() + " wants to login...");
            accessToken = oAuthClient.getToken(credentials.getUsername(), credentials.getPassword());
            keycloakUserID = new OpenIdConnectUserDetails(accessToken.getValue()).getUserId();
        } catch (OAuth2AccessDeniedException ex) {
            logger.error("User " + credentials.getUsername() + " not found in Keycloak?", ex);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (IOException ex) {
            logger.error("Error in decoding " + credentials.getUsername() + "'s access token", ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // check identity database
        UaaUser potentialUser = uaaUserRepository.findByExternalID(keycloakUserID);
        if (potentialUser == null) {
            logger.info("User " + credentials.getUsername() + " not found in local database, but on Keycloak.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create front end user DTO
        List<PartyType> companies = partyRepository.findByPerson(potentialUser.getUBLPerson());
        FrontEndUser frontEndUser = UblAdapter.adaptUser(potentialUser, companies);

        // set and store tokens
        frontEndUser.setAccessToken(accessToken.getValue());
        httpSession.setAttribute(REFRESH_TOKEN_SESSION_KEY, accessToken.getRefreshToken().getValue());

        logger.info("User " + credentials.getUsername() + " successfully logged in.");

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }


    @ApiOperation(value = "", notes = "Reset a users password")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Password reset was successful"),
        @ApiResponse(code = 400, message = "Password could not be reset")})
    @RequestMapping(value = "/reset-password", consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> resetPassword(
            @RequestHeader(value = "Authorization") String bearerToken,
            @ApiParam(value = "Old and new credentials", required = true) @RequestBody ResetPassword resetPasswordCredentials) throws IOException {

        UaaUser user = identityService.getUserfromBearer(bearerToken);

        // request password change
        boolean success = keycloakAdmin.resetPassword(user, resetPasswordCredentials.getOldPassword(), resetPasswordCredentials.getNewPassword());

        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "", notes = "Send email to reset the password")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Password reset email sent successfully"),
            @ApiResponse(code = 404, message = "Username not found")})
    @RequestMapping(value = "/password-recovery", consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> passwordRecovery(@ApiParam(value = "Email Account", required = true) @RequestBody ResetCredentials resetCredentials) throws IOException {
        logger.info("Password recovery request received for : {}", resetCredentials.getUsername());
        String token = keycloakAdmin.initiatePasswordRecoveryProcess(resetCredentials.getUsername());
        emailService.sendResetCredentialsLink(resetCredentials.getUsername(), token);
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "", notes = "Reset a users password with action token")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Password reset was successful"),
            @ApiResponse(code = 400, message = "Invalid link/token")})
    @RequestMapping(value = "/reset-forgot-password", consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> resetForgotPassword(@ApiParam(value = "Action token and new credentials", required = true) @RequestBody ResetCredentials resetCredentials) throws IOException {

        keycloakAdmin.resetPasswordViaRecoveryProcess(resetCredentials.getKey(), resetCredentials.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "", notes = "Get user info", response = Map.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Found user", response = Map.class),
            @ApiResponse(code = 404, message = "User not found", response = Map.class)})
    @RequestMapping(value = "/user-info", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization") String bearer) throws IOException {

        OpenIdConnectUserDetails userDetails = OpenIdConnectUserDetails.fromBearer(bearer);

        String username = userDetails.getUsername();
        Optional<UaaUser> potentialUaaUser = uaaUserRepository.findByUsername(username).stream().findFirst();

        if (potentialUaaUser.isPresent()) {
            Map<String, String> userInfo = new HashMap<>();
            UaaUser uaaUser = potentialUaaUser.get();
            PersonType personType = uaaUser.getUBLPerson();
            userInfo.put("ublPersonID", personType.getID());
            Optional<PartyType> potentialPartyType = partyRepository.findByPerson(personType).stream().findFirst();
            if (potentialPartyType.isPresent()) {
                PartyType partyType = potentialPartyType.get();
                userInfo.put("ublPartyID", UblAdapter.adaptPartyIdentifier(partyType));
            }

            return ResponseEntity.ok(userInfo);
        }

        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @ApiOperation(value = "", notes = "Set welcome info flag")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Changed flag"),
            @ApiResponse(code = 404, message = "User not found")})
    @RequestMapping(value = "/set-welcome-info/{flag}", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> setShowWelcomeInfoFlag(
            @ApiParam(value = "Show welcome info flag", required = true) @PathVariable Boolean flag,
            @RequestHeader(value = "Authorization") String bearer) throws IOException {

        UaaUser user = identityService.getUserfromBearer(bearer);
        user.setShowWelcomeInfo(flag);
        uaaUserRepository.save(user);

        return ResponseEntity.ok().build();
    }

    private String getKeycloakUserId(PersonType ublPerson) throws Exception {
        List<UaaUser> potentialUser = uaaUserRepository.findByUblPerson(ublPerson);
        UaaUser uaaUser = potentialUser.stream().findFirst().orElseThrow(() -> new Exception("Invalid user mapping"));
        return uaaUser.getExternalID();
    }

    private void informPlatformManager(PersonType representative, PartyType company) {
        List<UserRepresentation> managers = keycloakAdmin.getPlatformManagers();
        List<String> emails = managers.stream().map(UserRepresentation::getEmail).collect(Collectors.toList());

        emailService.notifyPlatformManagersNewCompany(emails, representative, company);
    }

    @ApiOperation(value = "Update user's favourite list of id's", response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Favourite id's successfully applied", response = String[].class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 400, message = "Error while applying roles")})
    @RequestMapping(value = "/favourite/{personId}", consumes = {"application/json"},produces = {"application/json"}, method = RequestMethod.PUT)
    ResponseEntity<?> setFavouriteIdList(
            @ApiParam(value = "Id of company to change settings from.", required = true) @PathVariable Long personId,
            @RequestParam("status") Integer status,
            @ApiParam(value = "Set of roles to apply.", required = true) @RequestBody List<String> itemIds) throws IOException {

        logger.debug("Requesting person favourite catalogue line id's for {}", personId);
        // search for persons
        List<PersonType> foundPersons = personRepository.findByHjid(personId);

        // check if person was found
        if (foundPersons.isEmpty()) {
            logger.info("Requested person with Id {} not found", personId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PersonType person = foundPersons.get(0);
        List<String> fhjids = person.getFavouriteProductID();

        if(status == 1){
            String exists =  fhjids.stream().filter(x -> x.equals(itemIds.get(0))).findAny().orElse(null);
            if(exists == null){
                fhjids.add(itemIds.get(0));
            }else {
                fhjids.remove(itemIds.get(0));
            }
        }else {
          fhjids.removeAll(itemIds);
        }
        person.setFavouriteProductID(fhjids);
        personRepository.save(person);
        return ResponseEntity.ok().build();

    }
}
