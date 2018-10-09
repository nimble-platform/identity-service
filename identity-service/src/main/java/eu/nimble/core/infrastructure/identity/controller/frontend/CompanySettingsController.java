package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanySettingsV2;
import eu.nimble.core.infrastructure.identity.messaging.KafkaSender;
import eu.nimble.core.infrastructure.identity.repository.CertificateRepository;
import eu.nimble.core.infrastructure.identity.repository.NegotiationSettingsRepository;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.QualifyingPartyRepository;
import eu.nimble.core.infrastructure.identity.service.CertificateService;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@RestController
@RequestMapping("/company-settings")
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Api(value = "company-settings", description = "API for handling settings of companies.")
public class CompanySettingsController {

    private static final Logger logger = LoggerFactory.getLogger(CompanySettingsController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private QualifyingPartyRepository qualifyingPartyRepository;

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

    @ApiOperation(value = "Retrieve company settings", response = CompanySettingsV2.class)
    @RequestMapping(value = "/{companyID}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<CompanySettingsV2> getSettings(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long companyID) {

        // search relevant parties
        Optional<PartyType> party = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (party.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<QualifyingPartyType> qualifyingPartyOptional = qualifyingPartyRepository.findByParty(party.get()).stream().findFirst();

        logger.debug("Returning requested settings for party with Id {}", party.get().getHjid());

        CompanySettingsV2 settings = UblAdapter.adaptCompanySettings(party.get(), qualifyingPartyOptional.orElse(null));
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @ApiOperation(value = "Change company settings")
    @RequestMapping(value = "/{companyID}", consumes = {"application/json"}, method = RequestMethod.PUT)
    ResponseEntity<CompanySettingsV2> setSettings(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company to change settings from.", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Settings to update.", required = true) @RequestBody CompanySettingsV2 newSettings) {

        // search relevant parties
        Optional<PartyType> partyOptional = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (partyOptional.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType existingCompany = partyOptional.get();
        logger.debug("Changing settings for party with Id {}", existingCompany.getHjid());

        existingCompany = UblAdapter.adaptCompanySettings(newSettings, null, existingCompany);

        Optional<QualifyingPartyType> qualifyingPartyOptional = qualifyingPartyRepository.findByParty(existingCompany).stream().findFirst();
        QualifyingPartyType qualifyingParty = UblAdapter.adaptQualifyingParty(newSettings, existingCompany, qualifyingPartyOptional.orElse(null));
        qualifyingPartyRepository.save(qualifyingParty);

        // set delivery terms
//        List<DeliveryTermsType> deliveryTerms = newSettings.getDeliveryTerms().stream().map(UblAdapter::adaptDeliveryTerms).collect(Collectors.toList());
//        if (party.getPurchaseTerms() == null)
//            party.setPurchaseTerms(new TradingPreferences());
//        party.getPurchaseTerms().setDeliveryTerms(deliveryTerms);   // ToDo: improve for sales terms
//
//        // set payment means
//        List<PaymentMeansType> paymentMeans = newSettings.getPaymentMeans().stream().map(UblAdapter::adaptPaymentMeans).collect(Collectors.toList());
//        party.getPurchaseTerms().setPaymentMeans(paymentMeans);   // ToDo: improve for sales terms

        // set preferred product categories
        List<CodeType> preferredProductCategories = UblAdapter.adaptProductCategories(newSettings.getPreferredProductCategories());
        existingCompany.setPreferredItemClassificationCode(preferredProductCategories);

        // set recently used product categories
        List<CodeType> recentlyUsedProductCategories = UblAdapter.adaptProductCategories(newSettings.getRecentlyUsedProductCategories());
        existingCompany.setMostRecentItemsClassificationCode(recentlyUsedProductCategories);

        partyRepository.save(existingCompany);

        // broadcast changes
        kafkaSender.broadcastCompanyUpdate(existingCompany.getID(), bearer);

        return new ResponseEntity<>(newSettings, HttpStatus.ACCEPTED);
    }

    @PostMapping("/certificate")
    public ResponseEntity<?> uploadCertificate(
            @RequestHeader(value = "Authorization") String bearer,
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("type") String type) throws IOException {

//        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
//            return new ResponseEntity<>("Only legal representatives are allowed add certificates", HttpStatus.UNAUTHORIZED);

        UaaUser user = identityUtils.getUserfromBearer(bearer);
        PartyType company = identityUtils.getCompanyOfUser(user).orElseThrow(CompanyNotFoundException::new);

        // create new certificate
        CertificateType certificate = UblAdapter.adaptCertificate(file, name, type, description);

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

    @ApiOperation(value = "", notes = "Fake changes of company")
    @RequestMapping(value = "/fake-changes/{partyId}", method = RequestMethod.GET)
    ResponseEntity<?> kafkaTest(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of party to fake changes.", required = true) @PathVariable String partyId) {

        this.kafkaSender.broadcastCompanyUpdate(partyId, bearer);

        return new ResponseEntity<>(HttpStatus.OK);
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
