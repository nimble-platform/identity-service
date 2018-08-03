package eu.nimble.core.infrastructure.identity.entity.dto;

import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@SuppressWarnings("unused")
public class CompanySettings {
    private String name;
    private String vatNumber;
    private String verificationInformation;
    private String website;
    private Address address;
    private List<PaymentMeans> paymentMeans = new ArrayList<>();
    private List<DeliveryTerms> deliveryTerms = new ArrayList<>();
    private Integer ppapCompatibilityLevel;
    private List<CompanyCertificate> certificates = new ArrayList<>();
    private Set<String> preferredProductCategories = new HashSet<>();
    private List<String> cnae  = new ArrayList<>();
    private List<String> industrySectors = new ArrayList<>();
    private NegotiationSettings negotiationSettings;

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

    public List<PaymentMeans> getPaymentMeans() {
        return paymentMeans;
    }

    public void setPaymentMeans(List<PaymentMeans> paymentMeans) {
        this.paymentMeans = paymentMeans;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<String> getCnae() {
        return cnae;
    }

    public void setCnae(List<String> cnae) {
        this.cnae = cnae;
    }

    public List<String> getIndustrySectors() {
        return industrySectors;
    }

    public void setIndustrySectors(List<String> industrySectors) {
        this.industrySectors = industrySectors;
    }

    public NegotiationSettings getNegotiationSettings() {
        return negotiationSettings;
    }

    public void setNegotiationSettings(NegotiationSettings negotiationSettings) {
        this.negotiationSettings = negotiationSettings;
    }
}
