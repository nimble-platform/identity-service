package eu.nimble.core.infrastructure.identity.uaa;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KeycloakAdmin {

    @Autowired
    private KeycloakConfig config;

    private Keycloak keycloak;

    @Autowired
    private OAuth2ClientContext oauth2Context;

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(config.getServerUrl())
                .realm(config.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .username(config.getAdmin().getUsername())
                .password(config.getAdmin().getPassword())
                .clientId(config.getAdmin().getCliendId())
                .clientSecret(config.getAdmin().getCliendSecret())
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build())
                .build();
    }

//    public void test() {
//        RealmResource realmResource = this.keycloak.realm(config.getRealm());
//        UsersResource userResource = realmResource.users();
//
//        // create new user
//        UserRepresentation user = new UserRepresentation();
//        user.setUsername("jinnerbichler");
//        user.setFirstName("firstname1");
//        user.setEmail("j.innerbichler@gmail.com");
////        user.setRequiredActions(Collections.emptyList());
//        user.setEnabled(true);
//        user.setEmailVerified(false);
//
//        // extract identifier of user
//        Response response = userResource.create(user);
//        String userId = extractCreatedId(response);
//        UserResource createdUser = userResource.get(userId);
//
//        // send verification mail
////        createdUser.sendVerifyEmail();
//
//        RoleRepresentation roleRepresentation = realmResource.roles().get("legal_representative").toRepresentation();
//        createdUser.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
//
//        System.out.println(realmResource.roles().list());
//
//        System.out.println(userResource.search(""));
//    }

    /**
     * throws javax.ws.rs.WebApplicationException with corrensponding response for error
     **/
    public String registerUser(String firstName, String lastName, String password, String email) {

        RealmResource realmResource = this.keycloak.realm(config.getRealm());
        UsersResource userResource = realmResource.users();

        // create proper credentials
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);

        // create user
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setCredentials(Collections.singletonList(passwordCredentials));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
//        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);
        user.setEmailVerified(false);

        // extract identifier of user
        Response response = userResource.create(user);
        String userId = extractCreatedId(response);
        UserResource createdUser = userResource.get(userId);

        // ToDo: throw exception if user already exists

        // set password
        createdUser.resetPassword(passwordCredentials);

        // send verification mail
//        createdUser.sendVerifyEmail();

        RoleRepresentation roleRepresentation = realmResource.roles().get("legal_representative").toRepresentation();
        createdUser.roles().realmLevel().add(Collections.singletonList(roleRepresentation));

        return createdUser.toRepresentation().getId();
    }

    private String extractCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            Response.StatusType statusInfo = response.getStatusInfo();
            throw new WebApplicationException("Create method returned status " +
                    statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201)",
                    statusInfo.getStatusCode());
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public OAuth2AccessToken getToken(String username, String password) {

        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
//        resourceDetails.setClientAuthenticationScheme(AuthenticationScheme.header);
        resourceDetails.setAccessTokenUri("http://localhost:10096/auth/realms/master/protocol/openid-connect/token");
        resourceDetails.setClientId("nimble-client");
        resourceDetails.setClientSecret("09bbdaaa-e76f-49c3-8808-9ac184e830a0");
        resourceDetails.setUsername(username);
        resourceDetails.setPassword(password);

        ResourceOwnerPasswordAccessTokenProvider accessTokenProvider = new ResourceOwnerPasswordAccessTokenProvider();

        AccessTokenRequest accessTokenRequest = oauth2Context.getAccessTokenRequest();

        OAuth2AccessToken accessToken = accessTokenProvider.obtainAccessToken(resourceDetails, accessTokenRequest);
        return accessToken;
    }
}
