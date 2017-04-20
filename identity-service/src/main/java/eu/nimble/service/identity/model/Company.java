package eu.nimble.service.identity.model;

import javax.persistence.*;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    private String companyName;
    private String companyAddress;

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public String getCompanyCountry() {
        return companyCountry;
    }

    private String companyCountry;

    protected Company() {}

    public Company(String companyName, String companyAddress, String companyCountry) {
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.companyCountry = companyCountry;
    }
}
