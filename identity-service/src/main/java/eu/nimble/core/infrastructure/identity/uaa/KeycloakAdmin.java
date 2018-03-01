package eu.nimble.core.infrastructure.identity.uaa;

import eu.nimble.core.infrastructure.identity.controller.frontend.UserIdentityController;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("Convert2MethodRef")
@Service
public class KeycloakAdmin {

    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);

    public static final String NIMBLE_USER_ROLE = "nimble_user";
    public static final String INITIAL_REPRESENTATIVE_ROLE = "initial_representative";
    public static final String LEGAL_REPRESENTATIVE_ROLE = "legal_representative";
    public static final String PLATFORM_MANAGER_ROLE = "platform_manager";

    @Autowired
    private KeycloakConfig config;

    private Keycloak keycloak;

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
        UserResource createdUser = fetchUserResource(userId);

        // ToDo: throw exception if user already exists

        // set password
        createdUser.resetPassword(passwordCredentials);

        // send verification mail
//        createdUser.sendVerifyEmail();

        addRole(userId, NIMBLE_USER_ROLE);

        return createdUser.toRepresentation().getId();
    }

    public Map<String, String> getRoles() {
        RealmResource realmResource = this.keycloak.realm(config.getRealm());

        List<String> invalidRoles = Arrays.asList("platform_manager", "uma_authorization", "offline_access",
                "admin", "create-realm", "create-realm", "nimble_user", "initial_representative");
        return realmResource.roles().list().stream()
                .filter(r -> invalidRoles.contains(r.getName()) == false)
                .collect(Collectors.toMap(r -> r.getId(), r -> r.getName()));
    }

    public Set<String> getUserRoles(String userId) {
        UserResource userResource = fetchUserResource(userId);
        return userResource.roles().realmLevel().listAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
    }

    public void addRole(String userId, String role) {
        UserResource userResource = fetchUserResource(userId);

        RealmResource realmResource = this.keycloak.realm(config.getRealm());
        RoleRepresentation roleRepresentation = realmResource.roles().get(role).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
    }

    public void removeRole(String userId, String role) {
        UserResource userResource = fetchUserResource(userId);

        RealmResource realmResource = this.keycloak.realm(config.getRealm());
        RoleRepresentation roleRepresentation = realmResource.roles().get(role).toRepresentation();
        userResource.roles().realmLevel().remove(Collections.singletonList(roleRepresentation));
    }

    public List<UserRepresentation> getPlatformManagers() {

        RealmResource realmResource = this.keycloak.realm(config.getRealm());
        List<GroupRepresentation> groups = realmResource.groups().groups();

        Optional<GroupRepresentation> platformManagerGroup = groups.stream().filter(g -> "Platform Manager".equals(g.getName())).findFirst();
        if (platformManagerGroup.isPresent() == false) {
            logger.warn("No platform managers found!");
            return new ArrayList<>();  // empty list as fallback
        }
        return realmResource.groups().group(platformManagerGroup.get().getId()).members();
    }

    private UserResource fetchUserResource(String userId) {
        RealmResource realmResource = this.keycloak.realm(config.getRealm());
        return realmResource.users().get(userId);
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
}
