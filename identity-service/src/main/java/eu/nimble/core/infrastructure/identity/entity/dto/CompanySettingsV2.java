package eu.nimble.core.infrastructure.identity.entity.dto;

import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Johannes Innerbichler on 02.10.18.
 */
public class CompanySettingsV2 {

    @ApiModelProperty(value = "Identifier of company")
    private Long companyID;// will be set after successful registration

    @ApiModelProperty(value = "General details related to the company")
    private CompanyDetails details = new CompanyDetails();

    @ApiModelProperty(value = "Descriptive data of the company")
    private CompanyDescription description = new CompanyDescription();

    @ApiModelProperty(value = "List of accepted payment means")
    private List<PaymentMeans> paymentMeans = new ArrayList<>();

    @ApiModelProperty(value = "List of accepted delivery terms")
    private List<DeliveryTerms> deliveryTerms = new ArrayList<>();

    @ApiModelProperty(value = "List of accepted delivery terms")
    private Integer ppapCompatibilityLevel;

    @ApiModelProperty(value = "List of company certificates")
    private List<CompanyCertificate> certificates = new ArrayList<>();

    @ApiModelProperty(value = "List of preferred product categories")
    private Set<String> preferredProductCategories = new HashSet<>();

    @ApiModelProperty(value = "List of recently used product categories")
    private Set<String> recentlyUsedProductCategories = new HashSet<>();

    public CompanySettingsV2() {
    }

    public CompanySettingsV2(Long companyID, CompanyDetails details, CompanyDescription description, List<PaymentMeans> paymentMeans,
                             List<DeliveryTerms> deliveryTerms, Integer ppapCompatibilityLevel, List<CompanyCertificate> certificates,
                             Set<String> preferredProductCategories, Set<String> recentlyUsedProductCategories) {
        this.companyID = companyID;
        this.details = details;
        this.description = description;
        this.paymentMeans = paymentMeans;
        this.deliveryTerms = deliveryTerms;
        this.ppapCompatibilityLevel = ppapCompatibilityLevel;
        this.certificates = certificates;
        this.preferredProductCategories = preferredProductCategories;
        this.recentlyUsedProductCategories = recentlyUsedProductCategories;
    }

    public Long getCompanyID() {
        return companyID;
    }

    public void setCompanyID(Long companyID) {
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

    public List<PaymentMeans> getPaymentMeans() {
        return paymentMeans;
    }

    public void setPaymentMeans(List<PaymentMeans> paymentMeans) {
        this.paymentMeans = paymentMeans;
    }

    public List<DeliveryTerms> getDeliveryTerms() {
        return deliveryTerms;
    }

    public void setDeliveryTerms(List<DeliveryTerms> deliveryTerms) {
        this.deliveryTerms = deliveryTerms;
    }

    public Integer getPpapCompatibilityLevel() {
        return ppapCompatibilityLevel;
    }

    public void setPpapCompatibilityLevel(Integer ppapCompatibilityLevel) {
        this.ppapCompatibilityLevel = ppapCompatibilityLevel;
    }

    public List<CompanyCertificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<CompanyCertificate> certificates) {
        this.certificates = certificates;
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
