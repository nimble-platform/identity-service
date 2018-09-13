package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class IdentityUtils {

    private static final Logger logger = LoggerFactory.getLogger(IdentityUtils.class);

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

    public boolean hasRole(String bearer, OAuthClient.Role role) throws IOException {
        OpenIdConnectUserDetails details = getUserDetails(bearer);
        return details.hasRole(role.toString());
    }

    public Optional<PartyType> getCompanyOfUser(UaaUser uaaUser) {
        return partyRepository.findByPerson(uaaUser.getUBLPerson()).stream().findFirst();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean inSameCompany(UaaUser userOne, UaaUser userTwo) {

        if (userOne == null || userTwo == null)
            return false;

        Optional<PartyType> requestingCompany = getCompanyOfUser(userOne);
        Optional<PartyType> targetCompany = getCompanyOfUser(userTwo);
        if ((requestingCompany.isPresent() && targetCompany.isPresent()) == false) // check if companies exist
            return false;

        return requestingCompany.get().getID().equals(targetCompany.get().getID());
    }

    public static PartyType removeBinaries(PartyType partyType) {
        for(CertificateType cert : partyType.getCertificate()) {
            cert.setDocumentReference(null);
        }
        return partyType;
    }
}
