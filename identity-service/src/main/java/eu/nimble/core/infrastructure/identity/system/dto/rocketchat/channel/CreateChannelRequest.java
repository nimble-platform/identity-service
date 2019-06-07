package eu.nimble.core.infrastructure.identity.system.dto.rocketchat.channel;

/**
 * Created by Nirojan Selvanathan on 05.06.19.
 */

public class CreateChannelRequest {

    private String userId;
    private String userToken;
    private String initiatingPartyID;
    private String respondingPartyID;
    private String productName;
    private String channelName;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getInitiatingPartyID() {
        return initiatingPartyID;
    }

    public void setInitiatingPartyID(String initiatingPartyID) {
        this.initiatingPartyID = initiatingPartyID;
    }

    public String getRespondingPartyID() {
        return respondingPartyID;
    }

    public void setRespondingPartyID(String respondingPartyID) {
        this.respondingPartyID = respondingPartyID;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}
