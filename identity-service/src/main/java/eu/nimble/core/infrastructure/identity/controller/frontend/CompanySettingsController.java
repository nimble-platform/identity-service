package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanySettings;
import eu.nimble.core.infrastructure.identity.messaging.KafkaSender;
import eu.nimble.core.infrastructure.identity.repository.CertificateRepository;
import eu.nimble.core.infrastructure.identity.repository.NegotiationSettingsRepository;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.service.CertificateService;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@RestController
@RequestMapping("/ ")
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Api(value = "company-settings", description = "API for handling settings of companies.")
public class CompanySettingsController {

    private static final Logger logger = LoggerFactory.getLogger(CompanySettingsController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private NegotiationSettingsRepository negotiationSettingsRepository;

    @Autowired
    private IdentityUtils identityUtils;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private KafkaSender kafkaSender;

    @ApiOperation(value = "Retrieve company settings", response = CompanySettings.class)
    @RequestMapping(value = "/{companyID}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<CompanySettings> getSettings(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long companyID) {

        // search relevant parties
        Optional<PartyType> party = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (party.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        logger.debug("Returning requested settings for party with Id {}", party.get().getHjid());


        CompanySettings settings = UblAdapter.adaptCompanySettings(party.get());
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @ApiOperation(value = "Change company settings")
    @RequestMapping(value = "/{companyID}", consumes = {"application/json"}, method = RequestMethod.PUT)
    ResponseEntity<CompanySettings> setSettings(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company to change settings from.", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Settings to update.", required = true) @RequestBody CompanySettings newSettings) {

        // search relevant parties
        Optional<PartyType> partyOptional = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (partyOptional.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType party = partyOptional.get();
        logger.debug("Changing settings for party with Id {}", party.getHjid());

        // set delivery terms
        List<DeliveryTermsType> deliveryTerms = newSettings.getDeliveryTerms().stream().map(UblAdapter::adaptDeliveryTerms).collect(Collectors.toList());
        if (party.getPurchaseTerms() == null)
            party.setPurchaseTerms(new TradingPreferences());
        party.getPurchaseTerms().setDeliveryTerms(deliveryTerms);   // ToDo: improve for sales terms

        // set payment means
        List<PaymentMeansType> paymentMeans = newSettings.getPaymentMeans().stream().map(UblAdapter::adaptPaymentMeans).collect(Collectors.toList());
        party.getPurchaseTerms().setPaymentMeans(paymentMeans);   // ToDo: improve for sales terms

        // set address
        AddressType companyAddress = UblAdapter.adaptAddress(newSettings.getAddress());
        party.setPostalAddress(companyAddress);

        // set default PPAP level
        int ppapLevel = newSettings.getPpapCompatibilityLevel() != null ? newSettings.getPpapCompatibilityLevel() : 0;
        party.setPpapCompatibilityLevel(BigDecimal.valueOf(ppapLevel));

        // set preferred product categories
        List<CodeType> preferredProductCategories = UblAdapter.adaptProductCategories(newSettings.getPreferredProductCategories());
        party.setPreferredItemClassificationCode(preferredProductCategories);

        // set recently used product categories
        List<CodeType> recentlyUsedProductCategories = UblAdapter.adaptProductCategories(newSettings.getRecentlyUsedProductCategories());
        party.setMostRecentItemsClassificationCode(recentlyUsedProductCategories);

        // set industry sector
        List<CodeType> industrySectors = UblAdapter.adaptIndustrySectors(newSettings.getIndustrySectors());
        party.setIndustrySector(industrySectors);

        // set miscellaneous
        party.setWebsiteURI(newSettings.getWebsite());
        List<PartyTaxSchemeType> partyTaxSchemes = new ArrayList<>();
        partyTaxSchemes.add(UblAdapter.adaptTaxSchema(newSettings.getVatNumber()));
        party.setPartyTaxScheme(partyTaxSchemes);
        List<QualityIndicatorType> qualityIndicators = new ArrayList<>();
//        qualityIndicators.add(UblAdapter.adaptQualityIndicator(newSettings.getVerificationInformation()));  // ToDo: add to QP
        party.setQualityIndicator(qualityIndicators);

        partyRepository.save(party);

        // broadcast changes
        kafkaSender.broadcastCompanyUpdate(party.getID(), bearer);

        return new ResponseEntity<>(newSettings, HttpStatus.ACCEPTED);
    }

    @PostMapping("/certificate")
    public ResponseEntity<?> uploadCertificate(
            @RequestHeader(value = "Authorization") String bearer,
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("type") String type) throws IOException {

//        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
//            return new ResponseEntity<>("Only legal representatives are allowed add certificates", HttpStatus.UNAUTHORIZED);

        UaaUser user = identityUtils.getUserfromBearer(bearer);
        PartyType company = identityUtils.getCompanyOfUser(user).orElseThrow(CompanyNotFoundException::new);

        // create new certificate
        CertificateType certificate = UblAdapter.adaptCertificate(file, name, type);

        // update and store company
        company.getCertificate().add(certificate);
        partyRepository.save(company);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Certificate download")
    @RequestMapping(value = "/certificate/{certificateId}", method = RequestMethod.GET)
    ResponseEntity<?> downloadCertificate(@ApiParam(value = "Id of certificate.", required = true) @PathVariable Long certificateId) {

        CertificateType certificateType = certificateService.queryCertificate(certificateId);
        if (certificateType == null)
            return ResponseEntity.notFound().build();

        BinaryObjectType certFile = certificateType.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject();
        Resource certResource = new ByteArrayResource(certFile.getValue());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certFile.getMimeCode()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certFile.getFileName() + "\"")
                .body(certResource);
    }

    @ApiOperation(value = "Certificate deletion")
    @RequestMapping(value = "/certificate/{certificateId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteCertificate(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of certificate.", required = true) @PathVariable Long certificateId) throws IOException {

        UaaUser user = identityUtils.getUserfromBearer(bearer);
        PartyType company = identityUtils.getCompanyOfUser(user).orElseThrow(CompanyNotFoundException::new);

        // update list of certificates
        List<CertificateType> filteredCerts = company.getCertificate().stream()
                .filter(c -> c.getHjid().equals(certificateId) == false)
                .collect(Collectors.toList());
        company.setCertificate(filteredCerts);
        partyRepository.save(company);

        // delete certificate
        certificateRepository.delete(certificateId);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Update negotiation settings")
    @RequestMapping(value = "/negotiation", method = RequestMethod.PUT)
    ResponseEntity<?> updateNegotiationSettings(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Settings to update.", required = true) @RequestBody NegotiationSettings newSettings) throws IOException {

        // find company
        UaaUser user = identityUtils.getUserfromBearer(bearer);
        PartyType company = identityUtils.getCompanyOfUser(user).orElseThrow(CompanyNotFoundException::new);

        // update settings
        NegotiationSettings existingSettings = findOrCreateNegotiationSettings(company);
        existingSettings.update(newSettings);
        existingSettings = negotiationSettingsRepository.save(existingSettings);

        logger.info("Updated negotiation settings {} for company {}", existingSettings.getId(), company.getID());

        // broadcast changes
        kafkaSender.broadcastCompanyUpdate(company.getID(), bearer);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Get negotiation settings", response = NegotiationSettings.class)
    @RequestMapping(value = "/negotiation/{companyID}", method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<?> getNegotiationSettings(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long companyID) {

        PartyType company = partyRepository.findByHjid(companyID).stream().findFirst().orElseThrow(CompanyNotFoundException::new);
        NegotiationSettings negotiationSettings = findOrCreateNegotiationSettings(company);

        logger.info("Fetched negotiation settings {} for company {}", negotiationSettings.getId(), company.getID());

        return ResponseEntity.ok().body(negotiationSettings);
    }

    private NegotiationSettings findOrCreateNegotiationSettings(PartyType company) {
        NegotiationSettings negotiationSettings = negotiationSettingsRepository.findByCompany(company).stream().findFirst().orElse(null);
        if (negotiationSettings == null) {
            negotiationSettings = new NegotiationSettings();
            negotiationSettings.setCompany(company);
            negotiationSettings = negotiationSettingsRepository.save(negotiationSettings);
        }
        return negotiationSettings;
    }

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "company not found")
    private static class CompanyNotFoundException extends RuntimeException {
    }
}
