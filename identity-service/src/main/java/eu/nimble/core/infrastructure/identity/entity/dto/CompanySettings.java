package eu.nimble.core.infrastructure.identity.entity.dto;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@SuppressWarnings("unused")
public class CompanySettings {
    private Address address;
    private PaymentMeans paymentMeans;
    private DeliveryTerms deliveryTerms;

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

    public DeliveryTerms getDeliveryTerms() {
        return deliveryTerms;
    }

    public void setDeliveryTerms(DeliveryTerms deliveryTerms) {
        this.deliveryTerms = deliveryTerms;
    }
}
