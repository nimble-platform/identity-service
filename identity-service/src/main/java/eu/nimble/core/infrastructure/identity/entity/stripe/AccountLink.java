package eu.nimble.core.infrastructure.identity.entity.stripe;

public class AccountLink {

    private String accountId;
    private String url;

    public AccountLink() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
