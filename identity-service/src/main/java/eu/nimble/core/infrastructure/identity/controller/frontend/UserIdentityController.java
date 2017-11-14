package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistrationResponse;
import eu.nimble.core.infrastructure.identity.controller.frontend.dto.UserRegistration;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

@Controller
public class UserIdentityController {

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
    private KeycloakAdmin keycloakAdmin;

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

        logger.info("Registering a new user with email {} and id {}", frontEndUser.getEmail(), frontEndUser.getUserID());

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }

    public @ApiOperation(value = "", notes = "Register company controller.", response = CompanyRegistration.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Company registered", response = CompanyRegistrationResponse.class),
            @ApiResponse(code = 405, message = "Company not registered", response = CompanyRegistrationResponse.class)})
    @RequestMapping(value = "/register/company", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<CompanyRegistration> registerCompany(
            @ApiParam(value = "Company object that needs to be registered to Nimble.", required = true) @RequestBody CompanyRegistration company) {

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

        return new ResponseEntity<>(company, HttpStatus.OK);
    }


    @ApiOperation(value = "", notes = "Login controller with credentials.", response = CompanyRegistrationResponse.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful login", response = CompanyRegistrationResponse.class),
            @ApiResponse(code = 401, message = "Login failed", response = CompanyRegistration.class)})
    @RequestMapping(value = "/login", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> loginUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody Credentials credentials,
            HttpServletResponse response) {

        OAuth2AccessToken accessToken = null;
        try {
            logger.info("User " + credentials.getUsername() + " wants to login...");
            accessToken = keycloakAdmin.getToken(credentials.getUsername(), credentials.getPassword());
        } catch (OAuth2AccessDeniedException ex) {
            logger.info("User " + credentials.getUsername() + " not found.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<UaaUser> potentialUsers = uaaUserRepository.findByUsername(credentials.getUsername());
        if (potentialUsers.isEmpty()) {
            logger.info("User " + credentials.getUsername() + " not found in local database, but on Keycloak.");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create front end user DTO
        UaaUser potentialUser = potentialUsers.get(0);
        List<PartyType> companies = partyRepository.findByPerson(potentialUser.getUBLPerson());
        FrontEndUser frontEndUser = UblAdapter.adaptUser(potentialUser, companies);

        // set cookie for OID token
        Cookie jwtCookie = new Cookie("OPENID_TOKEN", accessToken.getValue());
        jwtCookie.setMaxAge(accessToken.getExpiresIn()); // set expire time to expire time of access token
        response.addCookie(jwtCookie);

        logger.info("User " + credentials.getUsername() + " successfully logged in.");

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }
}