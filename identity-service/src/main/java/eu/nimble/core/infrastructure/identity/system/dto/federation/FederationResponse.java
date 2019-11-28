package eu.nimble.core.infrastructure.identity.system.dto.federation;

public class FederationResponse {

    private String ssoToken;
    private String endpoint;

    public String getSsoToken() {
        return ssoToken;
    }

    public void setSsoToken(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
