package eu.nimble.core.infrastructure.identity.clients;

import com.stripe.Stripe;
import com.stripe.exception.PermissionException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.LoginLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class StripeClient {

    @Value("${nimble.stripe.secretKey}")
    private String stripeSecretKey;

    @Value("${nimble.stripe.refreshUrl}")
    private String stripeRefreshUrl;

    @Value("${nimble.stripe.returnUrl}")
    private String stripeReturnUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public eu.nimble.core.infrastructure.identity.entity.stripe.AccountLink createAccount() throws StripeException {
        // create an Express account
        AccountCreateParams params =
                AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .build();

        Account account = Account.create(params);
        // return account link url
        return getAccountLink(account.getId());
    }

    public boolean deleteAccount(String accountId) throws StripeException {
        Account account = Account.retrieve(accountId);
        Account deletedAccount = account.delete();
        return deletedAccount.getDeleted();
    }

    public eu.nimble.core.infrastructure.identity.entity.stripe.AccountLink getAccountLink(String accountId) throws StripeException {
        // create an account link
        AccountLinkCreateParams accountLinkParams =
                AccountLinkCreateParams.builder()
                        .setAccount(accountId)
                        .setRefreshUrl(stripeRefreshUrl)
                        .setReturnUrl(stripeReturnUrl)
                        .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                        .build();

        AccountLink accountLink = AccountLink.create(accountLinkParams);

        eu.nimble.core.infrastructure.identity.entity.stripe.AccountLink stripeAccountLink = new eu.nimble.core.infrastructure.identity.entity.stripe.AccountLink();
        stripeAccountLink.setAccountId(accountId);
        stripeAccountLink.setUrl(accountLink.getUrl());
        // return account link url
        return stripeAccountLink;
    }

    public boolean validateAccount(String accountId) throws StripeException {
        try {
            Account account = Account.retrieve(accountId);
            return account.getDetailsSubmitted();
        } catch (PermissionException permissionException) {
            return false;
        }
    }

    public String getAccountLoginLink(String id) throws StripeException {
        boolean isAccountValid = validateAccount(id);
        if (isAccountValid) {
            LoginLink login_link = LoginLink.createOnAccount(
                    id,
                    (Map<String, Object>) null,
                    null
            );
            return login_link.getUrl();
        }
        return null;
    }
}