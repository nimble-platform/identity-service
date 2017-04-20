package eu.nimble.service.identity.uaa;

import org.cloudfoundry.identity.uaa.api.UaaConnectionFactory;
import org.cloudfoundry.identity.uaa.api.common.UaaConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class ConnectionFactory {

    @Value("${nimble.uaa.url}")
    private String uaaURL;

    public UaaConnection withClientCredentials(final String clientId, final String clientSecret) throws MalformedURLException {

        ClientCredentialsResourceDetails credentials = new ClientCredentialsResourceDetails();
        credentials.setAccessTokenUri(uaaURL + "/oauth/token");
        credentials.setClientAuthenticationScheme(AuthenticationScheme.header);
        credentials.setClientId(clientId);
        credentials.setClientSecret(clientSecret);

        URL uaaHost = new URL(uaaURL);
        return UaaConnectionFactory.getConnection(uaaHost, credentials);
    }
}
