package eu.nimble.core.identity.user;

import eu.nimble.core.identity.ConnectionFactory;
import eu.nimble.core.identity.ContextFactory;
import org.cloudfoundry.identity.uaa.api.common.model.expr.FilterRequest;
import org.cloudfoundry.identity.uaa.rest.SearchResults;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RestController;
import org.cloudfoundry.identity.uaa.api.common.UaaConnection;
import java.util.UUID;

import java.net.MalformedURLException;

@RestController
@RequestMapping("/user")
public class UserIdentityController {

    @Autowired
    private ContextFactory contextFactory;

    @Autowired
    private  ConnectionFactory connectionFactory;

    @RequestMapping(method=POST)
    public String addUser(){

        try {
            UaaConnection uaaConnection = connectionFactory.withClientCredentials("sultans",
                    "sultanssecret");

            // create user
            ScimUser newUser = new ScimUser(UUID.randomUUID().toString(), "user_name", "given_name", "family_name");
            newUser.addEmail("j.innerbichler@gmail.com");
            newUser.setPassword("password");
            uaaConnection.userOperations().createUser(newUser);

            // list user
            SearchResults<ScimUser> user = uaaConnection.userOperations().getUsers(new FilterRequest());

            for (ScimUser userDetails : user.getResources() ) {
                System.out.println(userDetails);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "ok";
    }
}
