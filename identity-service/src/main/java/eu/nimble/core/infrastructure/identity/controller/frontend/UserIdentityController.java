package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.core.infrastructure.identity.repository.*;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Optional;

@Controller
@SuppressWarnings("PointlessBooleanExpression")
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

    @ApiOperation(value = "Register a new controller to the nimble.", response = FrontEndUser.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User created", response = FrontEndUser.class),
            @ApiResponse(code = 405, message = "User not registered", response = FrontEndUser.class)})
    @RequestMapping(value = "/register/user", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> registerUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody UserRegistation userRegistation) {

        FrontEndUser frontEndUser = userRegistation.getUser();
        Credentials credentials = userRegistation.getCredentials();

        // validate data
        if (frontEndUser == null || credentials == null
                || credentials.getUsername() == null || credentials.getUsername().equals(frontEndUser.getEmail()) == false) {
            logger.info(" Tried to register an invalid user {}", userRegistation.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // check if user already exists
        if (uaaUserRepository.findByUsername(credentials.getUsername()).isEmpty() == false) {
            logger.info("User with username {} already exists", credentials.getUsername());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // create UBL party of user
        PersonType newUserParty = UblAdapter.adaptPerson(frontEndUser);
        personRepository.save(newUserParty);

        // update id of user
        newUserParty.setID(UblUtils.identifierType(newUserParty.getHjid()));
        personRepository.save(newUserParty);

        // create UAA user
        UaaUser uaaUser = new UaaUser(credentials.getUsername(), credentials.getPassword(), newUserParty);
        uaaUserRepository.save(uaaUser);

        // update user data
        frontEndUser.setUserID(Long.parseLong(newUserParty.getID()));
        frontEndUser.setUsername(credentials.getUsername());

        logger.info("Registering a new user with email {} and id {}", frontEndUser.getEmail(), frontEndUser.getUserID());

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }

    public @ApiOperation(value = "", notes = "Register company controller.", response = eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Company registered", response = eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration.class),
            @ApiResponse(code = 405, message = "Company not registered", response = eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration.class)})
    @RequestMapping(value = "/register/company", produces = {"application/json"}, consumes = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration> registerCompany(
            @ApiParam(value = "Company object that needs to be registered to Nimble.", required = true) @RequestBody eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration company) {

        Address companyAddress = company.getAddress();
        if( companyAddress == null || company.getName() == null )
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


    @ApiOperation(value = "", notes = "Login controller with credentials.", response = eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration.class, tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful login", response = eu.nimble.core.infrastructure.identity.controller.frontend.dto.CompanyRegistration.class),
            @ApiResponse(code = 401, message = "Login failed", response = CompanyRegistration.class)})
    @RequestMapping(value = "/login", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<FrontEndUser> loginUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody Credentials credentials) {

        logger.info("User " + credentials.getUsername() + " wants to login...");

        List<UaaUser> potentialUsers = uaaUserRepository.findByUsername(credentials.getUsername());
        if (potentialUsers.isEmpty()) {
            logger.info("User " + credentials.getUsername() + " not found.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UaaUser potentialUser = potentialUsers.get(0);
        if (potentialUser.getPassword().equals(credentials.getPassword()) == false) {
            logger.info("User " + credentials.getUsername() + " entered wrong password.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // create front end user DTO
        List<PartyType> companies = partyRepository.findByPerson(potentialUser.getUBLPerson());
        FrontEndUser frontEndUser = UblAdapter.adaptUser(potentialUser, companies);

        logger.info("User " + credentials.getUsername() + " successfully logged in.");

        return new ResponseEntity<>(frontEndUser, HttpStatus.OK);
    }

    private static class UserRegistation {

        private FrontEndUser user = null;
        private Credentials credentials = null;

        public FrontEndUser getUser() {
            return user;
        }

        public void setUser(FrontEndUser user) {
            this.user = user;
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public void setCredentials(Credentials credentials) {
            this.credentials = credentials;
        }

        @Override
        public String toString() {
            return "UserRegistation{" + "user=" + user + ", credentials=" + credentials + '}';
        }
    }
}