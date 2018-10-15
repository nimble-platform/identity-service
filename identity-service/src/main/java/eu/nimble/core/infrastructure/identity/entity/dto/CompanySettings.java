package eu.nimble.core.infrastructure.identity.entity.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Johannes Innerbichler on 02.10.18.
 */
public class CompanySettings {

    @ApiModelProperty(value = "Identifier of company")
    private String companyID;

    @ApiModelProperty(value = "General details related to the company")
    private CompanyDetails details = new CompanyDetails();

    @ApiModelProperty(value = "Descriptive data of the company")
    private CompanyDescription description = new CompanyDescription();

    @ApiModelProperty(value = "Trade details of the company")
    private CompanyTradeDetails tradeDetails = new CompanyTradeDetails();

    @ApiModelProperty(value = "Certificates of company")
    private List<CompanyCertificate> certificates = new ArrayList<>();

    @ApiModelProperty(value = "List of preferred product categories")
    private Set<String> preferredProductCategories = new HashSet<>();

    @ApiModelProperty(value = "List of recently used product categories")
    private Set<String> recentlyUsedProductCategories = new HashSet<>();

    public CompanySettings() {
    }

    public CompanySettings(String companyID, CompanyDetails details, CompanyDescription description, CompanyTradeDetails tradeDetails,
                           List<CompanyCertificate> certificates, Set<String> preferredProductCategories,
                           Set<String> recentlyUsedProductCategories) {
        this.companyID = companyID;
        this.details = details;
        this.description = description;
        this.tradeDetails = tradeDetails;
        this.certificates = certificates;
        this.preferredProductCategories = preferredProductCategories;
        this.recentlyUsedProductCategories = recentlyUsedProductCategories;
    }

    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(String companyID) {
        this.companyID = companyID;
    }

    public CompanyDetails getDetails() {
        return details;
    }

    public void setDetails(CompanyDetails details) {
        this.details = details;
    }

    public CompanyDescription getDescription() {
        return description;
    }

    public void setDescription(CompanyDescription description) {
        this.description = description;
    }

    public CompanyTradeDetails getTradeDetails() {
        return tradeDetails;
    }

    public List<CompanyCertificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<CompanyCertificate> certificates) {
        this.certificates = certificates;
    }

    public void setTradeDetails(CompanyTradeDetails tradeDetails) {
        this.tradeDetails = tradeDetails;
    }

    public Set<String> getPreferredProductCategories() {
        return preferredProductCategories;
    }

    public void setPreferredProductCategories(Set<String> preferredProductCategories) {
        this.preferredProductCategories = preferredProductCategories;
    }

    public Set<String> getRecentlyUsedProductCategories() {
        return recentlyUsedProductCategories;
    }

    public void setRecentlyUsedProductCategories(Set<String> recentlyUsedProductCategories) {
        this.recentlyUsedProductCategories = recentlyUsedProductCategories;
    }
}
