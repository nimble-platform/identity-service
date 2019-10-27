package eu.nimble.core.infrastructure.identity.system.dto.federation;

public class FederationResponse {

    private String sso_token;
    private String endpoint;

    public String getSso_token() {
        return sso_token;
    }

    public void setSso_token(String sso_token) {
        this.sso_token = sso_token;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
