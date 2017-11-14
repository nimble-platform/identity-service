package eu.nimble.core.infrastructure.identity.uaa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
@ConfigurationProperties(prefix = "nimble.oauth.client")
public class OAuthClientConfig {
    private String cliendId;
    private String cliendSecret;
    private String accessTokenUri;

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

    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }
}
