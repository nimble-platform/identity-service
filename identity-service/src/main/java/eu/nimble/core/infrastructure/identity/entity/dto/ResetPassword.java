package eu.nimble.core.infrastructure.identity.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by Johannes Innerbichler on 05.12.18.
 */
@ApiModel(value = "ResetPassword")
public class ResetPassword {

    @ApiModelProperty(value = "Existing password of user")
    private String oldPassword;

    @ApiModelProperty(value = "New password of user")
    private String newPassword;

    public ResetPassword() {
    }

    public ResetPassword(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
