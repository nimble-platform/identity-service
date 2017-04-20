package eu.nimble.service.identity.model;

import javax.persistence.*;

@Entity
@Table(name = "nimbleuser")
public class NimbleUser {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String jobTitle;
    private String email;
    private String dateOfBirth;
    private String placeOBirth;
    private String legalDomain;
    private String phoneNumber;

    protected NimbleUser() {}

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getEmail() {
        return email;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPlaceOBirth() {
        return placeOBirth;
    }

    public String getLegalDomain() {
        return legalDomain;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public NimbleUser(String username, String password, String firstname, String lastname, String jobTitle, String email, String dateOfBirth, String placeOBirth, String legalDomain, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.jobTitle = jobTitle;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.placeOBirth = placeOBirth;
        this.legalDomain = legalDomain;
        this.phoneNumber = phoneNumber;
    }
}
