package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistrationResponse;
import eu.nimble.core.infrastructure.identity.controller.frontend.dto.UserRegistration;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.entity.dto.Address;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyRegistration;
import eu.nimble.core.infrastructure.identity.entity.dto.Credentials;
import eu.nimble.core.infrastructure.identity.entity.dto.FrontEndUser;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PaymentMeansType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class UserIdentityController {

    private static final String REFRESH_TOKEN_SESSION_KEY = "refresh_token";

    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PartyRepository partyRepository;

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

    @ApiOperation(value = "Register a new user to the nimble.", response = FrontEndUser.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User created", response = FrontEndUser.class),
            @ApiResponse(code = 405, message = "User not registered", response = FrontEndUser.class)})
    @RequestMapping(value = "/register/user", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> registerUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody UserRegistration userRegistation) {

        FrontEndUser frontEndUser = userRegistation.getUser();
        Credentials credentials = userRegistation.getCredentials();

        // validate data
        if (frontEndUser == null || credentials == null
                || credentials.getUsername() == null || credentials.getUsername().equals(frontEndUser.getEmail()) == false) {
            logger.info(" Tried to register an invalid user {}", userRegistation.toString());
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
        PersonType newUserParty = UblAdapter.adaptPerson(frontEndUser);
        personRepository.save(newUserParty);

        // update id of user
        newUserParty.setID(UblUtils.identifierType(newUserParty.getHjid()));
        personRepository.save(newUserParty);

        // create entry in identity DB
        UaaUser uaaUser = new UaaUser(credentials.getUsername(), newUserParty, keycloakID);
        uaaUserRepository.save(uaaUser);

        // update user data
        frontEndUser.setUserID(Long.parseLong(newUserParty.getID()));
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
            company.getPerson().add(newUserParty);
            partyRepository.save(company);

            // save new state of invitation
            invitation.setPending(false);
            userInvitationRepository.save(invitation);

            logger.info("Invitation: added user {}({}) to company {}({})", frontEndUser.getEmail(), newUserParty.getID(), company.getName(), company.getID());
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
            @ApiParam(value = "Company object that needs to be registered to Nimble.", required = true) @RequestBody CompanyRegistration company,
            @RequestHeader(value = "Authorization") String bearer,
            HttpServletResponse response, HttpServletRequest request) {

        // update token
        String refreshToken = (String) httpSession.getAttribute(REFRESH_TOKEN_SESSION_KEY);
        if (refreshToken == null)
            return new ResponseEntity<>("Refresh token not found in session", HttpStatus.UNAUTHORIZED);

        Address companyAddress = company.getAddress();
        if (companyAddress == null || company.getName() == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Optional<PersonType> userPartyOpt = personRepository.findByHjid(company.getUserID()).stream().findFirst();

        // check if user exists
        if (userPartyOpt.isPresent() == false) {
            logger.info("Cannot find user with id {}", company.getUserID());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        PersonType userParty = userPartyOpt.get();

        // create company
        PartyType companyParty = UblAdapter.adaptCompanyRegistration(company, userParty);
        partyRepository.save(companyParty);

        // create delivery terms
        DeliveryTermsType blankDeliveryTerms = new DeliveryTermsType();
        deliveryTermsRepository.save(blankDeliveryTerms);
        blankDeliveryTerms.setID(UblUtils.identifierType(blankDeliveryTerms.getHjid()));
        deliveryTermsRepository.save(blankDeliveryTerms);
        companyParty.getDeliveryTerms().add(blankDeliveryTerms);

        // create payment means
        PaymentMeansType paymentMeans = UblUtils.emptyUBLObject(new PaymentMeansType());
        paymentMeansRepository.save(paymentMeans);
        paymentMeans.setID(UblUtils.identifierType(paymentMeans.getHjid()));
        paymentMeansRepository.save(paymentMeans);
        companyParty.getPaymentMeans().add(paymentMeans);

        // update id of company
        companyParty.setID(UblUtils.identifierType(companyParty.getHjid()));
        partyRepository.save(companyParty);

        // add id to original object
        company.setCompanyID(Long.parseLong(companyParty.getID()));
        company.setUserID(Long.parseLong(userParty.getID()));

        logger.info("Registered company with id {} for user with id {}", company.getCompanyID(), company.getUserID());

        // adapt role of user and refresh access token
        try {
            String keyCloakId = getKeycloakUserId(userParty);
            keycloakAdmin.addRole(keyCloakId, KeycloakAdmin.INITIAL_REPRESENTATIVE_ROLE);
        } catch (Exception e) {
            logger.error("Could not set role for user " + userParty.getID(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // update token
        OAuth2AccessToken updatedToken = oAuthClient.refreshToken(refreshToken);
        httpSession.setAttribute(REFRESH_TOKEN_SESSION_KEY, updatedToken.getRefreshToken().getValue());

        // set new access token
        company.setAccessToken(updatedToken.getValue());

        // inform platform managers
        try {
            informPlatformMangager(userParty, companyParty);
        } catch (Exception ex) {
            logger.error("Could not notify platform managers", ex);
        }

        return new ResponseEntity<>(company, HttpStatus.OK);
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
        try {
            logger.info("User " + credentials.getUsername() + " wants to login...");
            accessToken = oAuthClient.getToken(credentials.getUsername(), credentials.getPassword());
        } catch (OAuth2AccessDeniedException ex) {
            logger.info("User " + credentials.getUsername() + " not found.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // check identity database
        List<UaaUser> potentialUsers = uaaUserRepository.findByUsername(credentials.getUsername());
        if (potentialUsers.isEmpty()) {
            logger.info("User " + credentials.getUsername() + " not found in local database, but on Keycloak.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create front end user DTO
        UaaUser potentialUser = potentialUsers.get(0);
        List<PartyType> companies = partyRepository.findByPerson(potentialUser.getUBLPerson());
        FrontEndUser frontEndUser = UblAdapter.adaptUser(potentialUser, companies);

        // set and store tokens
        frontEndUser.setAccessToken(accessToken.getValue());
        httpSession.setAttribute(REFRESH_TOKEN_SESSION_KEY, accessToken.getRefreshToken().getValue());

        logger.info("User " + credentials.getUsername() + " successfully logged in.");

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }

    private String getKeycloakUserId(PersonType ublPerson) throws Exception {
        List<UaaUser> potentialUser = uaaUserRepository.findByUblPerson(ublPerson);
        UaaUser uaaUser = potentialUser.stream().findFirst().orElseThrow(() -> new Exception("Invalid user mapping"));
        return uaaUser.getExternalID();
    }

    private void informPlatformMangager(PersonType representative, PartyType company) {
        List<UserRepresentation> managers = keycloakAdmin.getPlatformManagers();
        List<String> emails = managers.stream().map(UserRepresentation::getEmail).collect(Collectors.toList());

        emailService.notifiyPlatformManagersNewCompany(emails, representative, company);
    }
}