package eu.nimble.core.infrastructure.identity.uaa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.stereotype.Service;

@Service
public class OAuthClient {

    @Autowired
    private OAuthClientConfig config;

    @Autowired
    private OAuth2ClientContext oauth2Context;

    @Bean
    public TokenStore tokenStore() {
        return new InMemoryTokenStore();
    }

    public OAuth2AccessToken getToken(String username, String password) {

        // build resources
        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setClientId(config.getCliendId());
        resourceDetails.setClientSecret(config.getCliendSecret());
        resourceDetails.setAccessTokenUri(config.getAccessTokenUri());
        resourceDetails.setUsername(username);
        resourceDetails.setPassword(password);

        // build provider
        ResourceOwnerPasswordAccessTokenProvider accessTokenProvider = new ResourceOwnerPasswordAccessTokenProvider();
        AccessTokenRequest accessTokenRequest = oauth2Context.getAccessTokenRequest();

        // fetch token
        OAuth2AccessToken accessToken = accessTokenProvider.obtainAccessToken(resourceDetails, accessTokenRequest);
        return accessToken;
    }

    public OAuth2AccessToken refreshToken(String refreshToken) {

        // build resources
        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setClientId(config.getCliendId());
        resourceDetails.setClientSecret(config.getCliendSecret());
        resourceDetails.setAccessTokenUri(config.getAccessTokenUri());

        // build provider
        AccessTokenRequest accessTokenRequest = oauth2Context.getAccessTokenRequest();
        ResourceOwnerPasswordAccessTokenProvider accessTokenProvider = new ResourceOwnerPasswordAccessTokenProvider();

        // perform refresh
        OAuth2AccessToken newAccessToken = accessTokenProvider.refreshAccessToken(resourceDetails, () -> refreshToken, accessTokenRequest);
        return newAccessToken;
    }

    public enum Role {
        NIMBLE_USER("nimble_user"),
        INITIAL_REPRESENTATIVE("initial_representative"),
        LEGAL_REPRESENTATIVE("legal_representative"),
        PLATFORM_MANAGER("platform_manager"),
        COMPANY_ADMIN("company_admin"),
        PUBLISHER("publisher"),
        NIMBLE_DELETED_USER("nimble_deleted_user"),
        EXTERNAL_REPRESENTATIVE("external_representative"),
        EFACTORY_USER("eFactoryUser");

        private final String role;

        Role(final String role) {
            this.role = role;
        }

        @Override
        public String toString() {
            return role;
        }
    }
}
