package eu.nimble.core.infrastructure.identity.mail.model;

import java.util.List;

/**
 * Summary of the company subscription.
 * Either {@code companyName} or {@code categoryName} is given based on the type of subscription i.e., company subscription or category subscription
 * */
public class SubscriptionSummary {
    private String companyName;
    private String categoryName;
    private List<String> catalogueIds;
    private List<String> productIds;

    public SubscriptionSummary() {
    }

    public SubscriptionSummary(String companyName, String categoryName, List<String> catalogueIds, List<String> productIds) {
        this.companyName = companyName;
        this.categoryName = categoryName;
        this.catalogueIds = catalogueIds;
        this.productIds = productIds;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<String> getCatalogueIds() {
        return catalogueIds;
    }

    public void setCatalogueIds(List<String> catalogueIds) {
        this.catalogueIds = catalogueIds;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }
}