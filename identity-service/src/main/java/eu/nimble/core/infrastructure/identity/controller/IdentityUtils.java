package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.Address;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyDescription;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyDetails;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyTradeDetails;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        if( uaaUser == null)
            return Optional.empty();
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

    public static Double computeDetailsCompleteness(CompanyDetails companyDetails) {

        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(StringUtils.isNotEmpty(companyDetails.getCompanyLegalName()) ? 1.0 : 0.0);
        completenessWeights.add(StringUtils.isNotEmpty(companyDetails.getVatNumber()) ? 1.0 : 0.0);
        completenessWeights.add(StringUtils.isNotEmpty(companyDetails.getVerificationInformation()) ? 1.0 : 0.0);
        Address address = companyDetails.getAddress();
        if (address != null) {
            completenessWeights.add(StringUtils.isNotEmpty(address.getStreetName()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getBuildingNumber()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getCityName()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getPostalCode()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getCountry()) ? 1.0 : 0.0);
        }
        completenessWeights.add(StringUtils.isNotEmpty(companyDetails.getBusinessType()) ? 1.0 : 0.0);
        completenessWeights.add(companyDetails.getBusinessKeywords() != null && companyDetails.getBusinessKeywords().size() > 0 ? 1.0 : 0.0);
        completenessWeights.add(companyDetails.getYearOfCompanyRegistration() != null ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d->d).average().orElse(0.0);
    }

    public static Double computeDescriptionCompleteness(CompanyDescription companyDescription) {
        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(StringUtils.isNotEmpty(companyDescription.getCompanyStatement()) ? 1.0 : 0.0);
        completenessWeights.add(StringUtils.isNotEmpty(companyDescription.getWebsite()) ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getSocialMediaList() != null && companyDescription.getSocialMediaList().size() > 0 ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getPastEvents() != null && companyDescription.getPastEvents().size() > 0 ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getUpcomingEvents() != null && companyDescription.getUpcomingEvents().size() > 0 ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d->d).average().orElse(0.0);
    }

    public static Double computeCertificateCompleteness(PartyType party) {
        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(party.getCertificate() != null && party.getCertificate().size() > 0 ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d->d).average().orElse(0.0);
    }

    public static Double computeTradeCompleteness(CompanyTradeDetails tradeDetails) {
        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(tradeDetails.getPpapCompatibilityLevel() != null && tradeDetails.getPpapCompatibilityLevel() > 0.0 ? 1.0: 0.0);
        completenessWeights.add(tradeDetails.getDeliveryTerms() != null && tradeDetails.getDeliveryTerms().size() > 0 ? 1.0 : 0.0);
        completenessWeights.add(tradeDetails.getPaymentMeans() != null && tradeDetails.getPaymentMeans().size() > 0 ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d->d).average().orElse(0.0);
    }
}
