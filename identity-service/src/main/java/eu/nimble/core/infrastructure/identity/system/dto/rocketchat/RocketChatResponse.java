package eu.nimble.core.infrastructure.identity.system.dto.rocketchat;

public class RocketChatResponse {
    private String loginToken;

    public String getLoginToken() {
        return loginToken;
    }

    public void setLoginToken(String loginToken) {
        this.loginToken = loginToken;
    }
}
