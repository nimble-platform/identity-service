package eu.nimble.core.infrastructure.identity.entity.dto;

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
}
