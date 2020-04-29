package eu.nimble.core.infrastructure.identity.system.dto.rocketchat.register;

public class RocketChatDeleteResponse {

    private boolean success = false;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}