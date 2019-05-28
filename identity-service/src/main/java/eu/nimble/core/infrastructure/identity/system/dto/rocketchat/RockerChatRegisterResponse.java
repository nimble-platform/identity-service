package eu.nimble.core.infrastructure.identity.system.dto.rocketchat;

public class RockerChatRegisterResponse {

    private boolean success = false;
    private RocketChatUser user;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public RocketChatUser getUser() {
        return user;
    }

    public void setUser(RocketChatUser user) {
        this.user = user;
    }
}
