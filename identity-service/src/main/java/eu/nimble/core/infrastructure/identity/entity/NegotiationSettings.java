package eu.nimble.core.infrastructure.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Johannes Innerbichler on 02.08.18.
 * Company wide business settings
 */
@Entity
@JsonInclude
public class NegotiationSettings implements Serializable {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PartyType company;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Range deliveryPeriodRange;

    @ElementCollection(targetClass=String.class)
    private List<String> validDeliveryPeriodUnits = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Range warrantyPeriodRange;

    @ElementCollection(targetClass=String.class)
    private List<String> warrantyPeriodUnits = new ArrayList<>();

    @ElementCollection(targetClass=String.class)
    private List<String> validIncoterms = new ArrayList<>();

    @ElementCollection(targetClass=String.class)
    private List<String> paymentTerms = new ArrayList<>();

    @ElementCollection(targetClass=String.class)
    private List<String> paymentMeans = new ArrayList<>();

    public NegotiationSettings(PartyType company, Range deliveryPeriodRange, List<String> validDeliveryPeriodUnits, Range warrantyPeriodRange,
                               List<String> warrantyPeriodUnits, List<String> validIncoterms, List<String> paymentTerms,
                               List<String> paymentMeans) {
        this.company = company;
        this.deliveryPeriodRange = deliveryPeriodRange;
        this.validDeliveryPeriodUnits = validDeliveryPeriodUnits;
        this.warrantyPeriodRange = warrantyPeriodRange;
        this.warrantyPeriodUnits = warrantyPeriodUnits;
        this.validIncoterms = validIncoterms;
        this.paymentTerms = paymentTerms;
        this.paymentMeans = paymentMeans;
    }

    public NegotiationSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PartyType getCompany() {
        return company;
    }

    public void setCompany(PartyType company) {
        this.company = company;
    }

    public Range getDeliveryPeriodRange() {
        return deliveryPeriodRange;
    }

    public void setDeliveryPeriodRange(Range deliveryPeriodRange) {
        this.deliveryPeriodRange = deliveryPeriodRange;
    }

    public List<String> getValidDeliveryPeriodUnits() {
        return validDeliveryPeriodUnits;
    }

    public void setValidDeliveryPeriodUnits(List<String> validDeliveryPeriodUnits) {
        this.validDeliveryPeriodUnits = validDeliveryPeriodUnits;
    }

    public Range getWarrantyPeriodRange() {
        return warrantyPeriodRange;
    }

    public void setWarrantyPeriodRange(Range warrantyPeriodRange) {
        this.warrantyPeriodRange = warrantyPeriodRange;
    }

    public List<String> getWarrantyPeriodUnits() {
        return warrantyPeriodUnits;
    }

    public void setWarrantyPeriodUnits(List<String> warrantyPeriodUnits) {
        this.warrantyPeriodUnits = warrantyPeriodUnits;
    }

    public List<String> getValidIncoterms() {
        return validIncoterms;
    }

    public void setValidIncoterms(List<String> validIncoterms) {
        this.validIncoterms = validIncoterms;
    }

    public List<String> getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(List<String> paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public List<String> getPaymentMeans() {
        return paymentMeans;
    }

    public void setPaymentMeans(List<String> paymentMeans) {
        this.paymentMeans = paymentMeans;
    }

    @Entity
    public static class Range {

        @Id
        @JsonIgnore
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column
        private Long start;

        @Column
        private Long end;

        public Range(long from, long to) {
            this.start = from;
            this.end = to;
        }

        public Range() {
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }
    }
}