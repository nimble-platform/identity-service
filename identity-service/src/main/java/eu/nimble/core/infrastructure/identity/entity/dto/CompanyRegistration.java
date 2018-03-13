package eu.nimble.core.infrastructure.identity.entity.dto;

public class CompanyRegistration   {

    private Long userID = null;

    private Long companyID = null; // will be set after successful registration

    private String name = null;

    private Address address = null;

    private String accessToken = null;

    private String vatNumber = null;

    private String verificationInformation = null;

    private String website = null;

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getVerificationInformation() {
        return verificationInformation;
    }

    public void setVerificationInformation(String verificationInformation) {
        this.verificationInformation = verificationInformation;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

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
}
