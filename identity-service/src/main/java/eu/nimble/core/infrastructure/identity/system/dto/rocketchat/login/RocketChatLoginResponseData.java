package eu.nimble.core.infrastructure.identity.system.dto.rocketchat.login;

public class RocketChatLoginResponseData {

    private String userId;
    private String authToken;
    private ChatUserInfo me;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public ChatUserInfo getMe() {
        return me;
    }

    public void setMe(ChatUserInfo me) {
        this.me = me;
    }
}
