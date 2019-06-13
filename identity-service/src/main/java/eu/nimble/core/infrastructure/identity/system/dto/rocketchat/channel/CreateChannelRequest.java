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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CreateChannelRequest {\n");

        sb.append("    initiatingPartyID: ").append(toIndentedString(initiatingPartyID)).append("\n");
        sb.append("    respondingPartyID: ").append(toIndentedString(respondingPartyID)).append("\n");
        sb.append("    productName: ").append(toIndentedString(productName)).append("\n");
        sb.append("    channelName: ").append(toIndentedString(channelName)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
