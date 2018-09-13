package eu.nimble.core.infrastructure.identity.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeliveryTerms {

    private String specialTerms;
    private Address deliveryAddress;
    private Integer estimatedDeliveryTime;

    public DeliveryTerms() {
    }

    public DeliveryTerms(String specialTerms, Address deliveryAddress, Integer estimatedDeliveryTime) {
        this.specialTerms = specialTerms;
        this.deliveryAddress = deliveryAddress;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public String getSpecialTerms() {
        return specialTerms;
    }

    public void setSpecialTerms(String specialTerms) {
        this.specialTerms = specialTerms;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Integer getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(Integer estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }
}
