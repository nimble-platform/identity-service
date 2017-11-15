package eu.nimble.core.infrastructure.identity.controller.frontend.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


/**
 * CompanyRegistrationResponse
 */

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class CompanyRegistrationResponse {
    private String username = null;

    private String password = null;

    private String firstname = null;

    private String lastname = null;

    private String jobTitle = null;

    private String email = null;

    private String dateOfBirth = null;

    private String placeOfBirth = null;

    private String legalDomain = null;

    private String phoneNumber = null;

    private String userID = null;

    private String companyName = null;

    private String companyAddress = null;

    private String companyCountry = null;

    private String companyID = null;

    private String accessToken = null;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public CompanyRegistrationResponse username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Get username
     *
     * @return username
     **/
    @ApiModelProperty(value = "")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public CompanyRegistrationResponse password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Get password
     *
     * @return password
     **/
    @ApiModelProperty(value = "")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CompanyRegistrationResponse firstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    /**
     * Get firstname
     *
     * @return firstname
     **/
    @ApiModelProperty(value = "")
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public CompanyRegistrationResponse lastname(String lastname) {
        this.lastname = lastname;
        return this;
    }

    /**
     * Get lastname
     *
     * @return lastname
     **/
    @ApiModelProperty(value = "")
    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public CompanyRegistrationResponse jobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        return this;
    }

    /**
     * Get jobTitle
     *
     * @return jobTitle
     **/
    @ApiModelProperty(value = "")
    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public CompanyRegistrationResponse email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Get email
     *
     * @return email
     **/
    @ApiModelProperty(value = "")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CompanyRegistrationResponse dateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    /**
     * Get dateOfBirth
     *
     * @return dateOfBirth
     **/
    @ApiModelProperty(value = "")
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public CompanyRegistrationResponse placeOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
        return this;
    }

    /**
     * Get placeOfBirth
     *
     * @return placeOfBirth
     **/
    @ApiModelProperty(value = "")
    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public CompanyRegistrationResponse legalDomain(String legalDomain) {
        this.legalDomain = legalDomain;
        return this;
    }

    /**
     * Get legalDomain
     *
     * @return legalDomain
     **/
    @ApiModelProperty(value = "")
    public String getLegalDomain() {
        return legalDomain;
    }

    public void setLegalDomain(String legalDomain) {
        this.legalDomain = legalDomain;
    }

    public CompanyRegistrationResponse phoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    /**
     * Get phoneNumber
     *
     * @return phoneNumber
     **/
    @ApiModelProperty(value = "")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public CompanyRegistrationResponse userID(String userID) {
        this.userID = userID;
        return this;
    }

    /**
     * Get userID
     *
     * @return userID
     **/
    @ApiModelProperty(value = "")
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public CompanyRegistrationResponse companyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    /**
     * Get companyName
     *
     * @return companyName
     **/
    @ApiModelProperty(value = "")
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public CompanyRegistrationResponse companyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
        return this;
    }

    /**
     * Get companyAddress
     *
     * @return companyAddress
     **/
    @ApiModelProperty(value = "")
    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public CompanyRegistrationResponse companyCountry(String companyCountry) {
        this.companyCountry = companyCountry;
        return this;
    }

    /**
     * Get companyCountry
     *
     * @return companyCountry
     **/
    @ApiModelProperty(value = "")
    public String getCompanyCountry() {
        return companyCountry;
    }

    public void setCompanyCountry(String companyCountry) {
        this.companyCountry = companyCountry;
    }

    public CompanyRegistrationResponse companyID(String companyID) {
        this.companyID = companyID;
        return this;
    }

    /**
     * Get companyID
     *
     * @return companyID
     **/
    @ApiModelProperty(value = "")
    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(String companyID) {
        this.companyID = companyID;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompanyRegistrationResponse CompanyRegistrationResponse = (CompanyRegistrationResponse) o;
        return Objects.equals(this.username, CompanyRegistrationResponse.username) &&
                Objects.equals(this.password, CompanyRegistrationResponse.password) &&
                Objects.equals(this.firstname, CompanyRegistrationResponse.firstname) &&
                Objects.equals(this.lastname, CompanyRegistrationResponse.lastname) &&
                Objects.equals(this.jobTitle, CompanyRegistrationResponse.jobTitle) &&
                Objects.equals(this.email, CompanyRegistrationResponse.email) &&
                Objects.equals(this.dateOfBirth, CompanyRegistrationResponse.dateOfBirth) &&
                Objects.equals(this.placeOfBirth, CompanyRegistrationResponse.placeOfBirth) &&
                Objects.equals(this.legalDomain, CompanyRegistrationResponse.legalDomain) &&
                Objects.equals(this.phoneNumber, CompanyRegistrationResponse.phoneNumber) &&
                Objects.equals(this.userID, CompanyRegistrationResponse.userID) &&
                Objects.equals(this.companyName, CompanyRegistrationResponse.companyName) &&
                Objects.equals(this.companyAddress, CompanyRegistrationResponse.companyAddress) &&
                Objects.equals(this.companyCountry, CompanyRegistrationResponse.companyCountry) &&
                Objects.equals(this.companyID, CompanyRegistrationResponse.companyID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, firstname, lastname, jobTitle, email, dateOfBirth, placeOfBirth, legalDomain, phoneNumber, userID, companyName, companyAddress, companyCountry, companyID);
    }

    @Override
    @SuppressWarnings("StringBufferReplaceableByString")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CompanyRegistrationResponse {\n");

        sb.append("    username: ").append(toIndentedString(username)).append("\n");
        sb.append("    password: ").append(toIndentedString(password)).append("\n");
        sb.append("    firstname: ").append(toIndentedString(firstname)).append("\n");
        sb.append("    lastname: ").append(toIndentedString(lastname)).append("\n");
        sb.append("    jobTitle: ").append(toIndentedString(jobTitle)).append("\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
        sb.append("    placeOfBirth: ").append(toIndentedString(placeOfBirth)).append("\n");
        sb.append("    legalDomain: ").append(toIndentedString(legalDomain)).append("\n");
        sb.append("    phoneNumber: ").append(toIndentedString(phoneNumber)).append("\n");
        sb.append("    userID: ").append(toIndentedString(userID)).append("\n");
        sb.append("    companyName: ").append(toIndentedString(companyName)).append("\n");
        sb.append("    companyAddress: ").append(toIndentedString(companyAddress)).append("\n");
        sb.append("    companyCountry: ").append(toIndentedString(companyCountry)).append("\n");
        sb.append("    companyID: ").append(toIndentedString(companyID)).append("\n");
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

