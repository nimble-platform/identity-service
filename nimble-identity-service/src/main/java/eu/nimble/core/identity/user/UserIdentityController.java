package eu.nimble.core.identity.user;

import eu.nimble.core.identity.ContextFactory;
import org.cloudfoundry.identity.client.UaaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

@RestController
@RequestMapping("/user")
public class UserIdentityController {

    @Value("${nimble.uaa.uri}")
    private String uaaURI;

    @Autowired
    private ContextFactory contextFactory;

    @RequestMapping(method=POST)
    public String addUser(){

        try {
            UaaContext context = contextFactory.withClientCredentials("sultans", "sultanssecret");

            ResourceOwnerPasswordResourceDetails credentials = new ResourceOwnerPasswordResourceDetails();
            credentials.setAccessTokenUri("http://localhost:8080/uaa/oauth/token");
            credentials.setClientAuthenticationScheme(AuthenticationScheme.header);
            credentials.setClientId("app");
            credentials.setClientSecret("appclientsecret");
            credentials.setUsername("myuser");
            credentials.setPassword("mypassword");

            URL uaaHost = new URL("http://localhost:8080/uaa");
            UaaConnection connection = UaaConnectionFactory.getConnection(uaaHost, credentials);
            UaaUserOperations operations = connection.userOperations();


        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "ok";
    }
}
