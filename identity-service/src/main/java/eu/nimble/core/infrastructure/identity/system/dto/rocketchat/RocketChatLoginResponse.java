package eu.nimble.core.infrastructure.identity.system.dto.rocketchat;

public class RocketChatLoginResponse {

    private String status;
    private RocketChatLoginResponseData data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RocketChatLoginResponseData getData() {
        return data;
    }

    public void setData(RocketChatLoginResponseData data) {
        this.data = data;
    }
}
