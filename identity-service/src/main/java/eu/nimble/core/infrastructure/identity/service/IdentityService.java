package eu.nimble.core.infrastructure.identity.service;

import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.Address;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyDescription;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyDetails;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyTradeDetails;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.service.model.ubl.commonaggregatecomponents.LocationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.TradingPreferences;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.*;

@Service
public class IdentityService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    public UaaUser getUserfromBearer(String bearer) throws IOException {
        OpenIdConnectUserDetails userDetails = getUserDetails(bearer);
        return uaaUserRepository.findByExternalID(userDetails.getUserId());
    }

    public OpenIdConnectUserDetails getUserDetails(String bearer) throws IOException {
        return OpenIdConnectUserDetails.fromBearer(bearer);
    }

    /**
     * Checks if the bearer contains at least one of the given roles.
     *
     * @param bearer Token containing roles
     * @param roles  Roles to check
     * @return True if at least one matching role was found.
     * @throws IOException if roles could not be extracted from token
     */
    public boolean hasAnyRole(String bearer, OAuthClient.Role... roles) throws IOException {
        OpenIdConnectUserDetails details = getUserDetails(bearer);
        return Arrays.stream(roles).anyMatch(r -> details.hasRole(r.toString()));
    }

    public Optional<PartyType> getCompanyOfUser(UaaUser uaaUser) {
        if (uaaUser == null)
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

        return requestingCompany.get().getHjid().equals(targetCompany.get().getHjid());
    }

    public Set<String> fetchRoles(PersonType personType) {
        try {
            UaaUser uaaUser = uaaUserRepository.findByUblPerson(personType).stream().findFirst().orElseThrow(NotFoundException::new);
            return fetchRoles(uaaUser);
        } catch (Exception exception) {
            logger.error("Error while fetching roles for person", exception);
        }
        return Collections.emptySet();
    }

    public Set<String> fetchRoles(UaaUser user) {

        Set<String> roles = new HashSet<>();
        try {
            // collect roles
            logger.debug("Fetching roles of user {}", user.getUsername());
            roles = keycloakAdmin.getUserRoles(user.getExternalID(), KeycloakAdmin.NON_NIMBLE_ROLES);
        } catch (Exception ex) {
            logger.error("Error while fetch roles of user", ex);
        }

        return roles;
    }

    public void enrichWithRoles(PersonType person) {
        Set<String> roles = fetchRoles(person);
        person.getRole().clear();
        person.getRole().addAll(roles);
    }

    public void enrichWithRoles(PartyType party) {
        party.getPerson().forEach(this::enrichWithRoles);
    }

    public static Double computeDetailsCompleteness(CompanyDetails companyDetails) {

        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(companyDetails.getLegalName().isEmpty() == false ? 1.0 : 0.0);
        completenessWeights.add(StringUtils.isNotEmpty(companyDetails.getVatNumber()) ? 1.0 : 0.0);
        completenessWeights.add(StringUtils.isNotEmpty(companyDetails.getBusinessType()) ? 1.0 : 0.0);
        completenessWeights.add(companyDetails.getIndustrySectors() != null && companyDetails.getIndustrySectors().size() > 0 ? 1.0 : 0.0);

        Address address = companyDetails.getAddress();
        if (address != null) {
            completenessWeights.add(StringUtils.isNotEmpty(address.getStreetName()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getCityName()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getPostalCode()) ? 1.0 : 0.0);
            completenessWeights.add(StringUtils.isNotEmpty(address.getCountry()) ? 1.0 : 0.0);
        }
        return completenessWeights.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public static Double computeDescriptionCompleteness(CompanyDescription companyDescription) {
        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(StringUtils.isNotEmpty(companyDescription.getCompanyStatement()) ? 1.0 : 0.0);
        completenessWeights.add(StringUtils.isNotEmpty(companyDescription.getWebsite()) ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getLogoImageId() != null ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getSocialMediaList() != null && companyDescription.getSocialMediaList().size() > 0 ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public static Double computeDeliveryAddressCompleteness(PartyType party) {
        List<Double> completenessWeights = new ArrayList<>();
        TradingPreferences tradingPreferences = party.getPurchaseTerms();
        if (tradingPreferences.getDeliveryTerms().size() == 0 || null == tradingPreferences.getDeliveryTerms().get(0).getDeliveryLocation()) {
            return 0.0;
        } else {
            LocationType locationType = tradingPreferences.getDeliveryTerms().get(0).getDeliveryLocation();
            completenessWeights.add(locationType.getAddress().getStreetName() != null ? 1.0 : 0.0);
            completenessWeights.add(locationType.getAddress().getBuildingNumber() != null ? 1.0 : 0.0);
            completenessWeights.add(locationType.getAddress().getCityName() != null ? 1.0 : 0.0);
            completenessWeights.add(locationType.getAddress().getRegion() != null ? 1.0 : 0.0);
            completenessWeights.add(locationType.getAddress().getPostalZone() != null ? 1.0 : 0.0);
            completenessWeights.add(locationType.getAddress().getCountry() != null ? 1.0 : 0.0);
            return completenessWeights.stream().mapToDouble(d -> d).average().orElse(0.0);
        }
    }

    public static Double computeCertificateCompleteness(PartyType party) {
        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(party.getCertificate() != null && party.getCertificate().size() > 0 ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public static Double computeTradeCompleteness(NegotiationSettings negotiationSettings) {
        List<Double> completenessWeights = new ArrayList<>();
        try {
            completenessWeights.add(negotiationSettings.getPaymentMeans() != null && negotiationSettings.getPaymentMeans().size() > 0 ? 1.0 : 0.0);
            completenessWeights.add(negotiationSettings.getPaymentTerms() != null && negotiationSettings.getPaymentTerms().size() > 0 ? 1.0 : 0.0);
            completenessWeights.add(negotiationSettings.getIncoterms() != null && negotiationSettings.getIncoterms().size() > 0 ? 1.0 : 0.0);
        }catch (Exception e){
            logger.error("Exception occurred while computing the tradCompletenessSocre", e);
        }
        return completenessWeights.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public static Double computeAdditionalDataCompleteness(PartyType party, CompanyTradeDetails tradeDetails, CompanyDescription companyDescription) {
        List<Double> completenessWeights = new ArrayList<>();
        completenessWeights.add(companyDescription.getEvents() != null && companyDescription.getEvents().size() > 0 ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getCompanyPhotoList() != null && companyDescription.getCompanyPhotoList().size() > 0 ? 1.0 : 0.0);
        completenessWeights.add(companyDescription.getExternalResources() != null && companyDescription.getExternalResources().size() > 0 ? 1.0 : 0.0);
        return completenessWeights.stream().mapToDouble(d -> d).average().orElse(0.0);
    }
}
