package eu.nimble.core.identity;

import org.cloudfoundry.identity.client.UaaContext;
import org.cloudfoundry.identity.client.UaaContextFactory;
import org.cloudfoundry.identity.client.token.GrantType;
import org.cloudfoundry.identity.client.token.TokenRequest;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class ContextFactory {

    @Value("${nimble.uaa.url}")
    private String uaaURI;

    private UaaContextFactory createFactory() throws URISyntaxException {
        return UaaContextFactory.factory(new URI(uaaURI))
                .authorizePath("/oauth/authorize")
                .tokenPath("/oauth/token");
    }

    public UaaContext withClientCredentials(final String clientId, final String clientSecret) throws URISyntaxException {
        UaaContextFactory factory = createFactory();
        TokenRequest clientCredentials = factory.tokenRequest()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setGrantType(GrantType.CLIENT_CREDENTIALS);
        return factory.authenticate(clientCredentials);
    }
}
