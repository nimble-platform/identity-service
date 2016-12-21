package eu.nimble.core.identity.user;

import eu.nimble.core.identity.uaa.ConnectionFactory;
import org.cloudfoundry.identity.uaa.api.group.UaaGroupOperations;
import org.cloudfoundry.identity.uaa.api.user.UaaUserOperations;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import org.cloudfoundry.identity.uaa.api.common.UaaConnection;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import java.net.MalformedURLException;

@RestController
@RequestMapping("/user")
public class UserIdentityController {

    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);

    @Value("${nimble.uaa.clientid}")
    private String uaaClientId;

    @Value("${nimble.uaa.clientsecret}")
    private String uaaClientSecret;

    @Autowired
    private ConnectionFactory connectionFactory;

    private UaaUserOperations uaaUserOperations;
    private UaaGroupOperations uaaGroupOperations;

    @PostConstruct
    public void init() throws MalformedURLException {

        UaaConnection uaaConnection = this.connectionFactory.withClientCredentials(uaaClientId, uaaClientSecret);

        this.uaaUserOperations = uaaConnection.userOperations();
        this.uaaGroupOperations = uaaConnection.groupOperations();
    }

    @RequestMapping(method = POST)
    public ResponseEntity<ScimUser> addUser(
            @RequestParam(value = "first_name") String firstName,
            @RequestParam(value = "family_name") String familyName,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "password") String password) { // TODO: send only hash of password?

        ScimUser newUser = new ScimUser(UUID.randomUUID().toString(), email, firstName, familyName);
        newUser.addEmail(email);
        newUser.setPassword(password);
        ScimUser createdUser = this.uaaUserOperations.createUser(newUser);

        logger.info("Created user '{}'", createdUser.toString());

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @RequestMapping(method = GET)
    public ScimUser getUser(@RequestParam(value = "user_name") String userName) throws UserNotFoundException {
        logger.info("Getting user with name '{}'", userName);
        ScimUser user = this.uaaUserOperations.getUserByName(userName);

        if (user == null )
            throw new UserNotFoundException();

        return user;
    }

    @RequestMapping(method = DELETE)
    public ResponseEntity<?> deleteUser(@RequestParam(value = "user_name") String userName) throws UserNotFoundException {
        logger.info("Deleting user with name '{}'", userName);
        ScimUser user = this.uaaUserOperations.getUserByName(userName);

        if (user == null )
            throw new UserNotFoundException();

        this.uaaUserOperations.deleteUser(user.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.GONE, reason = "This user is not found in the system")
    class UserNotFoundException extends Exception {
        private static final long serialVersionUID = 100L;
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<?> UaaExceptionHandler(HttpServletRequest req, HttpClientErrorException ex) {
        logger.error("Request: " + req.getRequestURL() + " raised " + ex);
        return new ResponseEntity<>("Error while communicating with UAA", null, ex.getStatusCode());
    }
}
