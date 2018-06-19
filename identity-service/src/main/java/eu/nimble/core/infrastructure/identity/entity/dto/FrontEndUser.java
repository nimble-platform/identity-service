package eu.nimble.core.infrastructure.identity.entity.dto;

import org.joda.time.LocalDate;

public class FrontEndUser {
    private String username = null;

    private String firstname = null;

    private String lastname = null;

    private String email = null;

    private LocalDate dateOfBirth = null;

    private String placeOBirth = null;

    private String phoneNumber = null;

    private Long userID = null;

    private String companyID = null;

    private String companyName = null;

    private String accessToken = null;

    private Boolean showWelcomeInfo = false;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPlaceOBirth() {
        return placeOBirth;
    }

    public void setPlaceOBirth(String placeOBirth) {
        this.placeOBirth = placeOBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(String companyID) {
        this.companyID = companyID;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Boolean getShowWelcomeInfo() {
        return showWelcomeInfo;
    }

    public void setShowWelcomeInfo(Boolean showWelcomeInfo) {
        this.showWelcomeInfo = showWelcomeInfo;
    }

    @Override
    public String toString() {
        return "FrontEndUser{" +
                "username='" + username + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", email='" + email + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", placeOBirth='" + placeOBirth + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", userID=" + userID +
                ", companyID='" + companyID + '\'' +
                ", companyName='" + companyName + '\'' +
                ", showWelcomeInfo=" + showWelcomeInfo +
                '}';
    }
}