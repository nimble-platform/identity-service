package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.service.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.persistence.initalizer.CustomDbInitializer;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import javax.servlet.http.HttpSession;
import java.io.IOException;

import static eu.nimble.core.infrastructure.identity.controller.IdentityController.REFRESH_TOKEN_SESSION_KEY;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Created by Johannes Innerbichler on 09.08.18.
 */
@Profile("test")
@TestConfiguration
public class DefaultTestConfiguration {

    @Bean
    @Primary
    public IdentityUtils identityResolver() throws IOException {

        IdentityUtils identityUtilsMock = Mockito.mock(IdentityUtils.class);

        // mock user query
        when(identityUtilsMock.getUserfromBearer(anyString())).thenReturn(new UaaUser());

        // mock company query
        PartyType mockParty = new PartyType();
        mockParty.setID("1");
        when(identityUtilsMock.getCompanyOfUser(anyObject())).thenReturn(java.util.Optional.of(mockParty));

        // mock verification of roles
        when(identityUtilsMock.hasAnyRole(any(), anyVararg())).thenReturn(true);

        return identityUtilsMock;
    }

    @Bean
    @Primary
    public KeycloakAdmin keycloakAdmin() {
        return Mockito.mock(KeycloakAdmin.class);
    }

    @Bean
    @Primary
    public EmailService emailService() {
        return Mockito.mock(EmailService.class);
    }

    @Bean
    @Primary
    public OAuthClient oAuthClient() {
        OAuthClient oAuthClient = Mockito.mock(OAuthClient.class);
        when(oAuthClient.refreshToken(anyString())).thenReturn(new DefaultOAuth2AccessToken("dummy token"));
        return oAuthClient;
    }

    @Bean
    @Primary
    public HttpSession httpSession() {
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        when(httpSession.getAttribute(same(REFRESH_TOKEN_SESSION_KEY))).thenReturn("random string");
        return httpSession;
    }

    @Bean
    @Primary
    public CustomDbInitializer customDbInitializer(){
        return Mockito.mock(CustomDbInitializer.class);
    }
}


