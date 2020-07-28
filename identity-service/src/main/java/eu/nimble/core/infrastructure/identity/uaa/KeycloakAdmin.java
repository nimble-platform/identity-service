package eu.nimble.core.infrastructure.identity.uaa;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.system.dto.oauth.RealmConfigs;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.rmi.ServerException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

@SuppressWarnings("Convert2MethodRef")
@Service
public class KeycloakAdmin {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdmin.class);

    public static final String NIMBLE_USER_ROLE = "nimble_user";
    public static final String INITIAL_REPRESENTATIVE_ROLE = "initial_representative";
    public static final String LEGAL_REPRESENTATIVE_ROLE = "legal_representative";
    public static final String PLATFORM_MANAGER_ROLE = "platform_manager";
    public static final String NIMBLE_DELETED_USER = "nimble_deleted_user";

    public static final String PLATFORM_MANAGER_GROUP = "Platform Manager";

    public static final List<String> NON_ASSIGNABLE_ROLES = Arrays.asList("platform_manager", "uma_authorization",
            "offline_access", "admin", "create-realm",
            "create-realm", "nimble_user", "initial_representative");

    public static final List<String> NON_NIMBLE_ROLES = Arrays.asList("uma_authorization", "offline_access", "admin", "create-realm", "create-realm");

    @Value("${nimble.oauth.identityProvider.eFactory}")
    private String eFactoryIdentityProvider;

    @Autowired
    private KeycloakConfig keycloakConfig;

    @Autowired
    private OAuthClient oAuthClient;

    @Autowired
    private OAuthClientConfig oAuthClientConfig;

    private Keycloak keycloak;

    private static long oneHourInMilliSeconds = 3600000;

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        ResteasyClient client = new ResteasyClientBuilder().connectionPoolSize(10).build();
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getServerUrl())
                .realm(keycloakConfig.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .username(keycloakConfig.getAdmin().getUsername())
                .password(keycloakConfig.getAdmin().getPassword())
                .clientId(keycloakConfig.getAdmin().getCliendId())
                .clientSecret(keycloakConfig.getAdmin().getCliendSecret())
                .resteasyClient(client)
                .build();
    }

    public String initiatePasswordRecoveryProcess(String email) {
        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
        UsersResource userResource = realmResource.users();

        List<UserRepresentation> userList = userResource.search(email);
        if (userList.size() != 0) {
            UserRepresentation user = userList.get(0);
            return JWT.create()
                    .withJWTId(user.getId())
                    .withClaim(GlobalConstants.JWT_TYPE_ATTRIBUTE_STRING, "reset-credentials")
                    .withSubject(user.getId())
                    .withExpiresAt(new Date(System.currentTimeMillis() + oneHourInMilliSeconds))
                    .sign(HMAC512(keycloakConfig.getAdmin().getCliendSecret().getBytes()));
        }else {
            throw new ResourceNotFoundException("Resource not found for the user name");
        }
    }

    public void resetPasswordViaRecoveryProcess(String token, String newPassword) {

        try {
            JWTVerifier verifier = JWT.require(HMAC512(keycloakConfig.getAdmin().getCliendSecret().getBytes())).build();
            verifier.verify(token);
        } catch (SignatureVerificationException e) {
            logger.warn("Invalid token received for password reset");
            throw new BadRequestException("Invalid Token");
        }

        long exp = JWT.decode(token).getClaim(GlobalConstants.JWT_EXPIRY_ATTRIBUTE_STRING).asLong();
        if (System.currentTimeMillis() > exp) {
            String sub = JWT.decode(token).getClaim(GlobalConstants.JWT_SUBJECT_ATTRIBUTE_STRING).asString();
            RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
            UserResource use = realmResource.users().get(sub);
            CredentialRepresentation passwordCredential = createPasswordCredentials(newPassword);
            passwordCredential.setTemporary(false);
            this.keycloak.realm(keycloakConfig.getRealm()).users().get(sub).resetPassword(passwordCredential);
        }else {
            throw new NotAuthorizedException("URL expired");
        }
    }

    /**
     * Verify the jwt token provided by the Keycloak
     * TODO: Implement verification by using the certs
     * https://stackoverflow.com/questions/39890232/how-to-decode-keys-from-keycloak-openid-connect-cert-api
     * @param jwtToken
     * @return
     */
    public boolean verify(String jwtToken) throws ServerException {

        String uri = keycloakConfig.getServerUrl() + "/realms/master";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        RestTemplate rs = new RestTemplate();

        try {
            ResponseEntity<String> response = rs.exchange(uri, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            RealmConfigs realmConfigs = mapper.readValue(response.getBody(), RealmConfigs.class);
            JwtHelper.decodeAndVerify(jwtToken, new RsaVerifier(getRSAPublicKey(realmConfigs.getPublic_key())));
        } catch (IOException e) {
            logger.error("Error in extracting the data from Keycloak realm response{}", e);
            throw new ServerException("Keycloak server error", e);
        } catch (Exception e) {
            logger.error("Error in verifying token{}", e);
            return false;
        }
        return true;
    }


    /**
     * Generates the RSA Public key from Keycloak public key
     * @param publicKey
     * @return
     * @throws Exception
     */
    private RSAPublicKey getRSAPublicKey(String publicKey) throws Exception {
        if( StringUtils.isBlank(publicKey)) return null;
        try {
            KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(java.util.Base64.getDecoder().decode(publicKey));
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            logger.error("Error forming RSA key {}", e);
            throw new Exception(e);
        }
    }

    /**
     * throws javax.ws.rs.WebApplicationException with corrensponding response for error
     **/
    public String registerUser(String firstName, String lastName, String password, String email) {

        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
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
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);
        user.setEmailVerified(false);

        // extract identifier of user
        Response response = userResource.create(user);
        String userId = extractCreatedId(response);
        UserResource createdUser = fetchUserResource(userId);

        // ToDo: throw exception if user already exists

        // set password
        createdUser.resetPassword(passwordCredentials);

//        // send verification mail
//        createdUser.executeActionsEmail(oAuthClientConfig.getCliendId(), "http://localhost:102", Collections.singletonList("VERIFY_EMAIL"));
//        createdUser.sendVerifyEmail(oAuthClientConfig.getCliendId());

        addRole(userId, NIMBLE_USER_ROLE);

        return createdUser.toRepresentation().getId();
    }

    public void deleteUser(String externalId) {
        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
        UsersResource userResource = realmResource.users();

        // delete user
        userResource.delete(externalId);
    }

    public void deleteUserByUsername(String username) {
        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
        UsersResource userResource = realmResource.users();

        List<UserRepresentation> userRepresentations = userResource.search(username);
        userRepresentations.forEach(userRepresentation -> userResource.delete(userRepresentation.getId()));
    }

    public Map<String, String> getAssignableRoles() {
        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());

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

    public UserResource getUserResource(String userId) {
        return fetchUserResource(userId);
    }

    public String getEFactoryUserId(UserResource userResource){
        for (FederatedIdentityRepresentation federatedIdentityRepresentation : userResource.getFederatedIdentity()) {
            if(federatedIdentityRepresentation.getIdentityProvider().contentEquals(eFactoryIdentityProvider)){
                    return federatedIdentityRepresentation.getUserId();
            }
        }
        return null;
    }
    public void addRole(String userId, String role) {
        UserResource userResource = fetchUserResource(userId);

        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
        RoleRepresentation roleRepresentation = realmResource.roles().get(role).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));
    }

    public void removeRole(String userId, String role) {
        UserResource userResource = fetchUserResource(userId);

        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
        RoleRepresentation roleRepresentation = realmResource.roles().get(role).toRepresentation();
        userResource.roles().realmLevel().remove(Collections.singletonList(roleRepresentation));
    }

    public List<UserRepresentation> getPlatformManagers() {

        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
        List<GroupRepresentation> groups = realmResource.groups().groups();

        Optional<GroupRepresentation> platformManagerGroup = groups.stream().filter(g -> "Platform Manager".equals(g.getName())).findFirst();
        if (platformManagerGroup.isPresent() == false) {
            logger.warn("No platform managers found!");
            return new ArrayList<>();  // empty list as fallback
        }
        return realmResource.groups().group(platformManagerGroup.get().getId()).members();
    }

    private UserResource fetchUserResource(String userId) {
        RealmResource realmResource = this.keycloak.realm(keycloakConfig.getRealm());
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
            this.keycloak.realm(keycloakConfig.getRealm()).users().get(user.getExternalID()).resetPassword(passwordCredential);
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
