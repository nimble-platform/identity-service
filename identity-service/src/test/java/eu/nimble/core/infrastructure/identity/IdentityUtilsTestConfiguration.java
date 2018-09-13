package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by Johannes Innerbichler on 09.08.18.
 */
@Profile("test")
@TestConfiguration
public class IdentityUtilsTestConfiguration {

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

        return identityUtilsMock;
    }
}


