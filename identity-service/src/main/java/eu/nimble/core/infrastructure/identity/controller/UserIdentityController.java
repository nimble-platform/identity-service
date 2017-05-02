package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.swagger.api.LoginApi;
import eu.nimble.core.infrastructure.identity.swagger.api.RegisterApi;
import eu.nimble.core.infrastructure.identity.swagger.api.RegisterCompanyApi;
import eu.nimble.core.infrastructure.identity.swagger.model.CompanyRegistration;
import eu.nimble.core.infrastructure.identity.swagger.model.Credentials;
import eu.nimble.core.infrastructure.identity.swagger.model.User;
import eu.nimble.core.infrastructure.identity.swagger.model.UserToRegister;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class UserIdentityController implements LoginApi, RegisterApi, RegisterCompanyApi {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Override
    public ResponseEntity<User> registerUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody UserToRegister user) {
        return null;
    }

    @Override
    public ResponseEntity<CompanyRegistration> registerCompany(
            @ApiParam(value = "Company object that needs to be registered to Nimble.", required = true) @RequestBody CompanyRegistration company) {

        PersonType adminPerson = new PersonType();
        adminPerson.setFirstName(company.getFirstname());
        adminPerson.setFamilyName(company.getLastname());

        personRepository.save(adminPerson);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @Override
    public ResponseEntity<CompanyRegistration> loginUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody Credentials credentials) {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}

//@RestController
//@RequestMapping("/controller")
//public class UserIdentityController {
//
//    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);
//
//    @Value("${nimble.uaa.clientid}")
//    private String uaaClientId;
//
//    @Value("${nimble.uaa.clientsecret}")
//    private String uaaClientSecret;
//
//    @Autowired
//    private ConnectionFactory connectionFactory;
//
//    private UaaUserOperations uaaUserOperations;
//    private UaaGroupOperations uaaGroupOperations;
//
//    @PostConstruct
//    public void init() throws MalformedURLException {
//
//        UaaConnection uaaConnection = this.connectionFactory.withClientCredentials(uaaClientId, uaaClientSecret);
//
//        this.uaaUserOperations = uaaConnection.userOperations();
//        this.uaaGroupOperations = uaaConnection.groupOperations();
//    }
//
//    @RequestMapping(method = POST)
//    public ResponseEntity<ScimUser> addUser(@RequestBody UserRegistrationData controller) { // TODO: send only hash of password?
//
//        // TODO: check password
//
//        ScimUser newUser = new ScimUser(UUID.randomUUID().toString(), controller.email, controller.firstname, controller.surname);
//        newUser.addEmail(controller.email);
//        newUser.setPassword(controller.password);
//        ScimUser createdUser = this.uaaUserOperations.createUser(newUser);
//
//        logger.info("Created controller '{}'", createdUser.toString());
//
//        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
//    }
//
//    @RequestMapping(method = GET)
//    public ScimUser getUser(@RequestParam(value = "user_name") String userName) throws UserNotFoundException {
//        logger.info("Getting controller with name '{}'", userName);
//        ScimUser controller = this.uaaUserOperations.getUserByName(userName);
//
//        if (controller == null )
//            throw new UserNotFoundException();
//
//        return controller;
//    }
//
//    @RequestMapping(method = DELETE)
//    public ResponseEntity<?> deleteUser(@RequestParam(value = "user_name") String userName) throws UserNotFoundException {
//        logger.info("Deleting controller with name '{}'", userName);
//        ScimUser controller = this.uaaUserOperations.getUserByName(userName);
//
//        if (controller == null )
//            throw new UserNotFoundException();
//
//        this.uaaUserOperations.deleteUser(controller.getId());
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @ResponseStatus(value = HttpStatus.GONE, reason = "This controller is not found in the system")
//    class UserNotFoundException extends Exception {
//        private static final long serialVersionUID = 100L;
//    }
//
//    @ExceptionHandler(HttpClientErrorException.class)
//    public ResponseEntity<?> UaaExceptionHandler(HttpServletRequest req, HttpClientErrorException ex) {
//        logger.error("Request: " + req.getRequestURL() + " raised " + ex);
//        return new ResponseEntity<>("Error while communicating with UAA", null, ex.getStatusCode());
//    }
//
//    private static class UserRegistrationData {
//        @JsonProperty(value = "firstname")
//        private String firstname;
//        @JsonProperty(value = "surname")
//        private String surname;
//        @JsonProperty(value = "password")
//        private String password;
//        @JsonProperty(value = "repassword")
//        private String repassword;
//        @JsonProperty(value = "email")
//        private String email;
//        @JsonProperty(value = "job-title")
//        private String jobTitle;
//        @JsonProperty(value = "date-of-birth")
//        private String dateOfBirth;
//        @JsonProperty(value = "place-of-birth")
//        private String placeOfBirth;
//        @JsonProperty(value = "legal-domain")
//        private String legalDomain;
//        @JsonProperty(value = "person-id")
//        private String personId;
//        @JsonProperty(value = "phone")
//        private String phone;
//    }
//}
