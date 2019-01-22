package eu.nimble.core.infrastructure.identity.entity.dto;

import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import io.swagger.annotations.ApiModelProperty;

import java.util.*;

/**
 * Created by Johannes Innerbichler on 25.09.18.
 */
public class CompanyDetails {

    @ApiModelProperty(value = "Legal name of the company")
    private Map<NimbleConfigurationProperties.LanguageID, String> companyLegalName = null;

    @ApiModelProperty(value = "VAT identification number of the company")
    private String vatNumber = null;

    @ApiModelProperty(value = "Additional info used for verifying the company")
    private String verificationInformation = null;

    @ApiModelProperty(value = "Main address of the company")
    private Address address = null;

    @ApiModelProperty(value = "Business type identifier")
    private String businessType = null;

    @ApiModelProperty(value = "Keywords explaining business objective")
    private Map<NimbleConfigurationProperties.LanguageID, String> businessKeywords = new HashMap<>();

    @ApiModelProperty(value = "Year since company is active")
    private Integer yearOfCompanyRegistration;

    @ApiModelProperty(value = "List of industry sectors in which company is active in.")
    private List<String> industrySectors = new ArrayList<>();

    public CompanyDetails() {
    }

    public CompanyDetails(Map<NimbleConfigurationProperties.LanguageID, String> companyLegalName, String vatNumber, String verificationInformation,
                          Address address, String businessType, Map<NimbleConfigurationProperties.LanguageID, String> businessKeywords,
                          Integer yearOfCompanyRegistration, List<String> industrySectors) {
        this.companyLegalName = companyLegalName;
        this.vatNumber = vatNumber;
        this.verificationInformation = verificationInformation;
        this.address = address;
        this.businessType = businessType;
        this.businessKeywords = businessKeywords;
        this.yearOfCompanyRegistration = yearOfCompanyRegistration;
        this.industrySectors = industrySectors;
    }

    public Map<NimbleConfigurationProperties.LanguageID, String> getCompanyLegalName() {
        return companyLegalName;
    }

    public void setCompanyLegalName(Map<NimbleConfigurationProperties.LanguageID, String> companyLegalName) {
        this.companyLegalName = companyLegalName;
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

    public List<String> getIndustrySectors() {
        return industrySectors;
    }

    public void setIndustrySectors(List<String> industrySectors) {
        this.industrySectors = industrySectors;
    }
}