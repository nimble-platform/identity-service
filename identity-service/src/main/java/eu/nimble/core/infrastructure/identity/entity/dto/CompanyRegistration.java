package eu.nimble.core.infrastructure.identity.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "CompanyRegistration")
public class CompanyRegistration   {

    @ApiModelProperty(value = "Identifier of requesting user", required = true)
    private Long userID;

    @ApiModelProperty(value = "Identifier of registered company (will be set in response)")
    private Long companyID;// will be set after successful registration

    @ApiModelProperty(value = "Updated access token after registration")
    private String accessToken;

    @ApiModelProperty(value = "Settings of company")
    private CompanySettings settings;

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public Long getCompanyID() {
        return companyID;
    }

    public void setCompanyID(Long companyID) {
        this.companyID = companyID;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public CompanySettings getSettings() {
        return settings;
    }

    public void setSettings(CompanySettings settings) {
        this.settings = settings;
    }
}
