package eu.nimble.core.infrastructure.identity.uaa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

@Service
public class OAuthClient {

    @Autowired
    private OAuthClientConfig config;

    @Autowired
    private OAuth2ClientContext oauth2Context;

    public OAuth2AccessToken getToken(String username, String password) {

        // build resources
        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setClientId(config.getCliendId());
        resourceDetails.setClientSecret(config.getCliendSecret());
        resourceDetails.setAccessTokenUri(config.getAccessTokenUri());
        resourceDetails.setUsername(username);
        resourceDetails.setPassword(password);

        // build provier
        ResourceOwnerPasswordAccessTokenProvider accessTokenProvider = new ResourceOwnerPasswordAccessTokenProvider();
        AccessTokenRequest accessTokenRequest = oauth2Context.getAccessTokenRequest();

        // fetch token
        return accessTokenProvider.obtainAccessToken(resourceDetails, accessTokenRequest);
    }
}
