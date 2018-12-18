package eu.nimble.core.infrastructure.identity.uaa;

import com.google.common.collect.Sets;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import org.apache.commons.lang.WordUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
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
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
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

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdmin.class);

    public static final String NIMBLE_USER_ROLE = "nimble_user";
    public static final String INITIAL_REPRESENTATIVE_ROLE = "initial_representative";
    public static final String LEGAL_REPRESENTATIVE_ROLE = "legal_representative";
    public static final String PLATFORM_MANAGER_ROLE = "platform_manager";

    public static final String PLATFORM_MANAGER_GROUP = "Platform Manager";

    public static final List<String> NON_ASSIGNABLE_ROLES = Arrays.asList("platform_manager", "uma_authorization",
            "offline_access", "admin", "create-realm",
            "create-realm", "nimble_user", "initial_representative");

    @Autowired
    private KeycloakConfig config;

    @Autowired
    private OAuthClient oAuthClient;

    private Keycloak keycloak;

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        ResteasyClient client = new ResteasyClientBuilder().connectionPoolSize(10).build();
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(config.getServerUrl())
                .realm(config.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .username(config.getAdmin().getUsername())
                .password(config.getAdmin().getPassword())
                .clientId(config.getAdmin().getCliendId())
                .clientSecret(config.getAdmin().getCliendSecret())
                .resteasyClient(client)
                .build();
    }

    /**
     * throws javax.ws.rs.WebApplicationException with corrensponding response for error
     **/
    public String registerUser(String firstName, String lastName, String password, String email) {

        RealmResource realmResource = this.keycloak.realm(config.getRealm());
        UsersResource userResource = realmResource.users();

        // create proper credentials
        CredentialRepresentation passwordCredentials = createPasswordCredentials(password);

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

    public Map<String, String> getAssignableRoles() {
        RealmResource realmResource = this.keycloak.realm(config.getRealm());

        return realmResource.roles().list().stream()
                .filter(r -> NON_ASSIGNABLE_ROLES.contains(r.getName()) == false)
                .collect(Collectors.toMap(r -> r.getId(), r -> r.getName()));
    }

    public Set<String> getUserRoles(String userId) {
        return getUserRoles(userId, new ArrayList<>());
    }

    public Set<String> getUserRoles(String userId, List<String> excludeRoles) {

        List<String> finalExcludeRoles = (excludeRoles == null) ? new ArrayList<>() : excludeRoles;
        UserResource userResource = fetchUserResource(userId);
        return userResource.roles().realmLevel().listAll().stream()
                .map(r -> r.getName())
                .filter(role -> finalExcludeRoles.contains(role) == false)
                .collect(Collectors.toSet());
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

    public boolean resetPassword(UaaUser user, String oldPassword, String newPassword) {

        try {
            // verify existing password
            OAuth2AccessToken accessToken = oAuthClient.getToken(user.getUsername(), oldPassword);
            if (accessToken == null)
                return false;

            // set new password
            CredentialRepresentation passwordCredential = createPasswordCredentials(newPassword);
            this.keycloak.realm(config.getRealm()).users().get(user.getExternalID()).resetPassword(passwordCredential);
        } catch (OAuth2AccessDeniedException ex) {
            logger.info("Authentication error while setting new password");
            return false;
        }

        return true;
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

    public int applyRoles(String userID, Set<String> rolesToApply) {
        // setting proper set of roles
        Set<String> currentRoles = getUserRoles(userID, NON_ASSIGNABLE_ROLES);
        Set<String> rolesToRemove = Sets.difference(currentRoles, rolesToApply);
        Set<String> rolesToAdd = Sets.difference(rolesToApply, currentRoles);
        logger.info("Applying new roles to user {}: add: {}, remove: {}", userID, rolesToAdd, rolesToRemove);
        for (String role : rolesToRemove)
            removeRole(userID, role);
        for (String role : rolesToAdd)
            addRole(userID, role);
        return rolesToAdd.size() + rolesToRemove.size();
    }

    public static List<String> prettfiyRoleIDs(List<String> roleIDs) {
        return roleIDs.stream().map(r -> WordUtils.capitalize(r.replace("_", " "))).collect(Collectors.toList());
    }

    private static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }
}
