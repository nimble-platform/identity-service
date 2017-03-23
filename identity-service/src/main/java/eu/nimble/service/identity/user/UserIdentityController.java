package eu.nimble.service.identity.user;

import eu.nimble.service.identity.repository.CustomerRepository;
import eu.nimble.service.identity.swagger.api.LoginApi;
import eu.nimble.service.identity.swagger.api.RegisterApi;
import eu.nimble.service.identity.swagger.api.RegisterCompanyApi;
import eu.nimble.service.identity.swagger.model.CompanyRegistration;
import eu.nimble.service.identity.swagger.model.User;
import eu.nimble.service.identity.swagger.model.UserToRegister;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserIdentityController implements LoginApi, RegisterApi, RegisterCompanyApi {

    @Autowired
    CustomerRepository repository;

    @Override
    public ResponseEntity<User> loginUser(
            @ApiParam(value = "Name of user") @RequestParam(value = "username", required = false) String username,
            @ApiParam(value = "Password of user") @RequestParam(value = "password", required = false) String password) {

//        repository.save(new Customer("Johannes", "Innerbichler"));

        return null;
    }

    @Override
    public ResponseEntity<User> registerUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody UserToRegister user) {


        return null;
    }

    @Override
    public ResponseEntity<CompanyRegistration> registerCompany(
            @ApiParam(value = "Company object that needs to be registered to Nimble.", required = true) @RequestBody CompanyRegistration company) {
        return null;
    }
}

//@RestController
//@RequestMapping("/user")
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
//    public ResponseEntity<ScimUser> addUser(@RequestBody UserRegistrationData user) { // TODO: send only hash of password?
//
//        // TODO: check password
//
//        ScimUser newUser = new ScimUser(UUID.randomUUID().toString(), user.email, user.firstname, user.surname);
//        newUser.addEmail(user.email);
//        newUser.setPassword(user.password);
//        ScimUser createdUser = this.uaaUserOperations.createUser(newUser);
//
//        logger.info("Created user '{}'", createdUser.toString());
//
//        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
//    }
//
//    @RequestMapping(method = GET)
//    public ScimUser getUser(@RequestParam(value = "user_name") String userName) throws UserNotFoundException {
//        logger.info("Getting user with name '{}'", userName);
//        ScimUser user = this.uaaUserOperations.getUserByName(userName);
//
//        if (user == null )
//            throw new UserNotFoundException();
//
//        return user;
//    }
//
//    @RequestMapping(method = DELETE)
//    public ResponseEntity<?> deleteUser(@RequestParam(value = "user_name") String userName) throws UserNotFoundException {
//        logger.info("Deleting user with name '{}'", userName);
//        ScimUser user = this.uaaUserOperations.getUserByName(userName);
//
//        if (user == null )
//            throw new UserNotFoundException();
//
//        this.uaaUserOperations.deleteUser(user.getId());
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @ResponseStatus(value = HttpStatus.GONE, reason = "This user is not found in the system")
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
