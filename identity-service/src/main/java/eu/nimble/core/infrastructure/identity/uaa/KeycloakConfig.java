package eu.nimble.core.infrastructure.identity.uaa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
@ConfigurationProperties(prefix = "nimble.keycloak")
public class KeycloakConfig {

    private String serverUrl;
    private String realm;

    private final Admin admin = new Admin();

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Admin getAdmin() {
        return admin;
    }

    public static class Admin {
        String username;
        String password;
        String cliendId;
        String cliendSecret;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCliendId() {
            return cliendId;
        }

        public void setCliendId(String cliendId) {
            this.cliendId = cliendId;
        }

        public String getCliendSecret() {
            return cliendSecret;
        }

        public void setCliendSecret(String cliendSecret) {
            this.cliendSecret = cliendSecret;
        }
    }
}
