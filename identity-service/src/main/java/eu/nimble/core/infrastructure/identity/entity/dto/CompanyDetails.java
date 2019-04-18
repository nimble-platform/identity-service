package eu.nimble.core.infrastructure.identity.entity.dto;

import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties.LanguageID;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Johannes Innerbichler on 25.09.18.
 */
public class CompanyDetails {

    @ApiModelProperty(value = "Trade name name of the company")
    private Map<LanguageID, String> brandName = null;

    @ApiModelProperty(value = "Legal name of the company")
    private Map<LanguageID, String> legalName = null;

    @ApiModelProperty(value = "VAT identification number of the company")
    private String vatNumber = null;

    @ApiModelProperty(value = "Additional info used for verifying the company")
    private String verificationInformation = null;

    @ApiModelProperty(value = "Main address of the company")
    private Address address = null;

    @ApiModelProperty(value = "Business type identifier")
    private String businessType = null;

    @ApiModelProperty(value = "Keywords explaining business objective")
    private Map<LanguageID, String> businessKeywords = new HashMap<>();

    @ApiModelProperty(value = "Year since company is active")
    private Integer yearOfCompanyRegistration;

    @ApiModelProperty(value = "List of industry sectors in which company is active in.")
    private Map<LanguageID, String> industrySectors = new HashMap<>();

    public CompanyDetails() {
    }

    public CompanyDetails(Map<LanguageID, String> brandName, Map<LanguageID, String> legalName, String vatNumber,
                          String verificationInformation, Address address, String businessType, Map<LanguageID, String> businessKeywords,
                          Integer yearOfCompanyRegistration, Map<LanguageID, String> industrySectors) {
        this.brandName = brandName;
        this.legalName = legalName;
        this.vatNumber = vatNumber;
        this.verificationInformation = verificationInformation;
        this.address = address;
        this.businessType = businessType;
        this.businessKeywords = businessKeywords;
        this.yearOfCompanyRegistration = yearOfCompanyRegistration;
        this.industrySectors = industrySectors;
    }

    public Map<LanguageID, String> getBrandName() {
        return brandName;
    }

    public void setBrandName(Map<LanguageID, String> brandName) {
        this.brandName = brandName;
    }

    public Map<LanguageID, String> getLegalName() {
        return legalName;
    }

    public void setLegalName(Map<LanguageID, String> legalName) {
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

    public Map<LanguageID, String> getBusinessKeywords() {
        return businessKeywords;
    }

    public void setBusinessKeywords(Map<LanguageID, String> businessKeywords) {
        this.businessKeywords = businessKeywords;
    }

    public Integer getYearOfCompanyRegistration() {
        return yearOfCompanyRegistration;
    }

    public void setYearOfCompanyRegistration(Integer yearOfCompanyRegistration) {
        this.yearOfCompanyRegistration = yearOfCompanyRegistration;
    }

    public Map<LanguageID, String> getIndustrySectors() {
        return industrySectors;
    }

    public void setIndustrySectors(Map<LanguageID, String> industrySectors) {
        this.industrySectors = industrySectors;
    }
}
