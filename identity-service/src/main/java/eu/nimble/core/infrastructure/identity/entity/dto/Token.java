package eu.nimble.core.infrastructure.identity.entity.dto;

/**
 * Created by Nirojan Selvanathan on 20/05/19.
 */
public class Token {

    private String accessToken = null;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
