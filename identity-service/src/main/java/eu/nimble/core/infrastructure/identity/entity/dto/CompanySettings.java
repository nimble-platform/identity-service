package eu.nimble.core.infrastructure.identity.entity.dto;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
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

    @ApiModelProperty(value = "Set of subscribed company ids")
    private Set<String> subscribedCompanyIds = new HashSet<>();

    @ApiModelProperty(value = "List of subscribed category codes")
    private List<CodeType> subscribedProductCategories = new ArrayList<>();

    @ApiModelProperty(value = "List of terms and conditions files")
    private List<DocumentReferenceType> termsAndConditions = new ArrayList<>();

    public CompanySettings() {
    }

    public CompanySettings(String companyID, CompanyDetails details, CompanyDescription description, CompanyTradeDetails tradeDetails,
                           List<CompanyCertificate> certificates, Set<String> preferredProductCategories,
                           Set<String> recentlyUsedProductCategories,Set<String> subscribedCompanyIds,List<CodeType> subscribedProductCategories) {
        this.companyID = companyID;
        this.details = details;
        this.description = description;
        this.tradeDetails = tradeDetails;
        this.certificates = certificates;
        this.preferredProductCategories = preferredProductCategories;
        this.recentlyUsedProductCategories = recentlyUsedProductCategories;
        this.subscribedCompanyIds = subscribedCompanyIds;
        this.subscribedProductCategories = subscribedProductCategories;
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

    public Set<String> getSubscribedCompanyIds() {
        return subscribedCompanyIds;
    }

    public void setSubscribedCompanyIds(Set<String> subscribedCompanyIds) {
        this.subscribedCompanyIds = subscribedCompanyIds;
    }

    public List<CodeType> getSubscribedProductCategories() {
        return subscribedProductCategories;
    }

    public void setSubscribedProductCategories(List<CodeType> subscribedProductCategories) {
        this.subscribedProductCategories = subscribedProductCategories;
    }

    public List<DocumentReferenceType> getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(List<DocumentReferenceType> termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }
}
