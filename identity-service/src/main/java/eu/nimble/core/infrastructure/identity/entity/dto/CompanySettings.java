package eu.nimble.core.infrastructure.identity.entity.dto;

import java.util.ArrayList;
import java.util.List;

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
    private PaymentMeans paymentMeans;
    private DeliveryTerms deliveryTerms;
    private Integer ppapCompatibilityLevel;
    private List<CompanyCertificate> certificates = new ArrayList();

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

    public PaymentMeans getPaymentMeans() {
        return paymentMeans;
    }

    public void setPaymentMeans(PaymentMeans paymentMeans) {
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

    public DeliveryTerms getDeliveryTerms() {
        return deliveryTerms;
    }

    public void setDeliveryTerms(DeliveryTerms deliveryTerms) {
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
}
