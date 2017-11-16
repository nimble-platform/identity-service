package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class IdentityUtils {

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private PartyRepository partyRepository;

    public UaaUser getUserfromBearer(String bearer) throws IOException {
        OpenIdConnectUserDetails userDetails = getUserDetails(bearer);
        return uaaUserRepository.findByExternalID(userDetails.getUserId());
    }

    public OpenIdConnectUserDetails getUserDetails(String bearer) throws IOException {
        return OpenIdConnectUserDetails.fromBearer(bearer);
    }

    public Optional<PartyType> getCompanyOfUser(UaaUser uaaUser) {
        return partyRepository.findByPerson(uaaUser.getUBLPerson()).stream().findFirst();
    }
}
