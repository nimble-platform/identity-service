package eu.nimble.core.infrastructure.identity.mail.model;

import java.util.List;

/**
 * Model corresponding to the subscription variable in mail template.
 * */
public class SubscriptionMailModel {
    private String title; // title for the subscription
    private List<String> productUrls; // urls for product details

    public SubscriptionMailModel() {
    }

    public SubscriptionMailModel(String title,List<String> productUrls) {
        this.title = title;
        this.productUrls = productUrls;
    }

    public List<String> getProductUrls() {
        return productUrls;
    }

    public void setProductUrls(List<String> productUrls) {
        this.productUrls = productUrls;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
