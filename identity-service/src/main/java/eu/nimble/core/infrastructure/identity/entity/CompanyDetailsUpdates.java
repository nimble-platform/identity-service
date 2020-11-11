package eu.nimble.core.infrastructure.identity.entity;

import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.entity.dto.Address;

import java.util.Map;

/**
 * Model to store updated fields of company details
 */
public class CompanyDetailsUpdates {

    private Map<NimbleConfigurationProperties.LanguageID, String> brandName = null;

    private Map<NimbleConfigurationProperties.LanguageID, String> legalName = null;

    private String vatNumber = null;

    private String verificationInformation = null;

    private Address address = null;

    private String businessType = null;

    private Map<NimbleConfigurationProperties.LanguageID, String> businessKeywords = null;

    private Integer yearOfCompanyRegistration = null;

    private Map<NimbleConfigurationProperties.LanguageID, String> industrySectors = null;


    public CompanyDetailsUpdates() {
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getBrandName() {
        return brandName;
    }

    public void setBrandName(Map<NimbleConfigurationProperties.LanguageID, String> brandName) {
        this.brandName = brandName;
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getLegalName() {
        return legalName;
    }

    public void setLegalName(Map<NimbleConfigurationProperties.LanguageID, String> legalName) {
        this.legalName = legalName;
    }

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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getBusinessKeywords() {
        return businessKeywords;
    }

    public void setBusinessKeywords(Map<NimbleConfigurationProperties.LanguageID, String> businessKeywords) {
        this.businessKeywords = businessKeywords;
    }

    public Integer getYearOfCompanyRegistration() {
        return yearOfCompanyRegistration;
    }

    public void setYearOfCompanyRegistration(Integer yearOfCompanyRegistration) {
        this.yearOfCompanyRegistration = yearOfCompanyRegistration;
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getIndustrySectors() {
        return industrySectors;
    }

    public void setIndustrySectors(Map<NimbleConfigurationProperties.LanguageID, String> industrySectors) {
        this.industrySectors = industrySectors;
    }

    public boolean areCompanyDetailsUpdated() {
        return this.brandName != null || this.address != null || this.businessKeywords != null || this.businessType != null || this.industrySectors != null ||
                this.legalName != null || this.vatNumber != null || this.verificationInformation != null || this.yearOfCompanyRegistration != null;
    }

}
