package eu.nimble.core.infrastructure.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;

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
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"id", "company_hjid"})})
public class NegotiationSettings implements Serializable {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private PartyType company;

    @JoinTable
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Range> deliveryPeriodRanges = new ArrayList<>();

    @ElementCollection(targetClass = String.class)
    private List<String> deliveryPeriodUnits = new ArrayList<>();

    @JoinTable
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Range> warrantyPeriodRanges = new ArrayList<>();

    @ElementCollection(targetClass = String.class)
    private List<String> warrantyPeriodUnits = new ArrayList<>();

    @ElementCollection(targetClass = String.class)
    private List<String> incoterms = new ArrayList<>();

    @ElementCollection(targetClass = String.class)
    private List<String> paymentTerms = new ArrayList<>();

    @ElementCollection(targetClass = String.class)
    private List<String> paymentMeans = new ArrayList<>();

    public NegotiationSettings(PartyType company, List<Range> deliveryPeriodRanges, List<String> deliveryPeriodUnits, List<Range> warrantyPeriodRange,
                               List<String> warrantyPeriodUnits, List<String> incoterms, List<String> paymentTerms, List<String> paymentMeans) {
        this.company = company;
        this.deliveryPeriodRanges = deliveryPeriodRanges;
        this.deliveryPeriodUnits = deliveryPeriodUnits;
        this.warrantyPeriodRanges = warrantyPeriodRange;
        this.warrantyPeriodUnits = warrantyPeriodUnits;
        this.incoterms = incoterms;
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

    public List<Range> getDeliveryPeriodRanges() {
        return deliveryPeriodRanges;
    }

    public void setDeliveryPeriodRanges(List<Range> deliveryPeriodRanges) {
        this.deliveryPeriodRanges = deliveryPeriodRanges;
    }

    public List<String> getDeliveryPeriodUnits() {
        return deliveryPeriodUnits;
    }

    public void setDeliveryPeriodUnits(List<String> deliveryPeriodUnits) {
        this.deliveryPeriodUnits = deliveryPeriodUnits;
    }

    public List<Range> getWarrantyPeriodRanges() {
        return warrantyPeriodRanges;
    }

    public void setWarrantyPeriodRanges(List<Range> warrantyPeriodRanges) {
        this.warrantyPeriodRanges = warrantyPeriodRanges;
    }

    public List<String> getWarrantyPeriodUnits() {
        return warrantyPeriodUnits;
    }

    public void setWarrantyPeriodUnits(List<String> warrantyPeriodUnits) {
        this.warrantyPeriodUnits = warrantyPeriodUnits;
    }

    public List<String> getIncoterms() {
        return incoterms;
    }

    public void setIncoterms(List<String> incoterms) {
        this.incoterms = incoterms;
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

    public void update(NegotiationSettings newSettings) {
        this.deliveryPeriodRanges.clear();
        this.deliveryPeriodRanges.addAll(newSettings.getDeliveryPeriodRanges());

        this.deliveryPeriodUnits.clear();
        this.deliveryPeriodUnits.addAll(newSettings.getDeliveryPeriodUnits());

        this.warrantyPeriodRanges.clear();
        this.warrantyPeriodRanges.addAll(newSettings.getWarrantyPeriodRanges());

        this.warrantyPeriodUnits.clear();
        this.warrantyPeriodUnits.addAll(newSettings.getWarrantyPeriodUnits());

        this.incoterms.clear();
        this.incoterms.addAll(newSettings.getIncoterms());

        this.paymentMeans.clear();
        this.paymentMeans.addAll(newSettings.getPaymentMeans());

        this.paymentTerms.clear();
        this.paymentTerms.addAll(newSettings.getPaymentTerms());
    }

    @Entity
    public static class Range {

        @Id
        @JsonIgnore
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column
        @JsonProperty("start")
        @SerializedName("start")
        private Long rangeStart;

        @Column
        @JsonProperty("end")
        @SerializedName("end")
        private Long rangeEnd;

        public Range(long start, long end) {
            this.rangeStart = start;
            this.rangeEnd = end;
        }

        public Range() {
        }

        public long getRangeStart() {
            return rangeStart;
        }

        public long getRangeEnd() {
            return rangeEnd;
        }
    }
}
