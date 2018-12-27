package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanySettings;
import eu.nimble.core.infrastructure.identity.messaging.KafkaSender;
import eu.nimble.core.infrastructure.identity.repository.*;
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static eu.nimble.core.infrastructure.identity.utils.UblAdapter.*;
import static eu.nimble.service.model.ubl.extension.QualityIndicatorParameter.*;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@RestController
@RequestMapping("/company-settings")
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Api(value = "company-settings", description = "API for handling settings of companies.")
public class CompanySettingsController {

    private final Long MAX_IMAGE_SIZE = 256L * 1024L; // in bytes

    private static final Logger logger = LoggerFactory.getLogger(CompanySettingsController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private QualifyingPartyRepository qualifyingPartyRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DocumentReferenceRepository documentReferenceRepository;

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
        Optional<PartyType> partyOptional = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (partyOptional.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PartyType party = partyOptional.get();

        Optional<QualifyingPartyType> qualifyingPartyOptional = qualifyingPartyRepository.findByParty(partyOptional.get()).stream().findFirst();

        logger.debug("Returning requested settings for party with Id {}", party.getHjid());

        // pre fetch image metadata without binaries
        enrichImageMetadata(party);

        CompanySettings settings = UblAdapter.adaptCompanySettings(party, qualifyingPartyOptional.orElse(null));
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

        PartyType existingCompany = partyOptional.get();
        logger.debug("Changing settings for party with Id {}", existingCompany.getHjid());

        existingCompany = UblAdapter.adaptCompanySettings(newSettings, null, existingCompany);

        Optional<QualifyingPartyType> qualifyingPartyOptional = qualifyingPartyRepository.findByParty(existingCompany).stream().findFirst();
        QualifyingPartyType qualifyingParty = UblAdapter.adaptQualifyingParty(newSettings, existingCompany, qualifyingPartyOptional.orElse(null));
        qualifyingPartyRepository.save(qualifyingParty);

        // set preferred product categories
        List<CodeType> preferredProductCategories = UblAdapter.adaptProductCategories(newSettings.getPreferredProductCategories());
        existingCompany.setPreferredItemClassificationCode(preferredProductCategories);

        // set recently used product categories
        List<CodeType> recentlyUsedProductCategories = UblAdapter.adaptProductCategories(newSettings.getRecentlyUsedProductCategories());
        existingCompany.setMostRecentItemsClassificationCode(recentlyUsedProductCategories);

        partyRepository.save(existingCompany);

        // broadcast changes
        kafkaSender.broadcastCompanyUpdate(existingCompany.getID(), bearer);

        newSettings = adaptCompanySettings(existingCompany, qualifyingParty);
        return new ResponseEntity<>(newSettings, HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Upload company image")
    @RequestMapping(value = "/image", produces = {"application/json"}, method = RequestMethod.POST)
    public ResponseEntity<DocumentReferenceType> uploadImage(
            @RequestHeader(value = "Authorization") String bearer,
            @RequestParam(value = "isLogo", defaultValue = "false") String isLogo,
            @RequestParam(value = "file") MultipartFile imageFile) throws IOException {

//        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
//            return new ResponseEntity<>("Only legal representatives are allowed add images", HttpStatus.UNAUTHORIZED);

        if (imageFile.getSize() > MAX_IMAGE_SIZE)
            throw new FileTooLargeException();

        UaaUser user = identityUtils.getUserfromBearer(bearer);
        PartyType company = identityUtils.getCompanyOfUser(user).orElseThrow(CompanyNotFoundException::new);

        logger.info("Storing image for company with ID " + company.getID());

        Boolean logoFlag = "true".equals(isLogo);
        DocumentReferenceType imageDocument = UblAdapter.adaptCompanyPhoto(imageFile, logoFlag);
        documentReferenceRepository.save(imageDocument);

        company.getDocumentReference().add(imageDocument);
        partyRepository.save(company);

        imageDocument.getAttachment().setEmbeddedDocumentBinaryObject(null);
        imageDocument.setID(imageDocument.getHjid().toString());
        return ResponseEntity.ok(imageDocument);
    }

    @ApiOperation(value = "Download company image")
    @RequestMapping(value = "/image/{imageId}", produces = {"application/json"}, method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadImage(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long imageId) {

        // collect image resource
        DocumentReferenceType imageDocument = documentReferenceRepository.findOne(imageId);
        if (imageDocument == null)
            throw new DocumentNotFoundException();
        BinaryObjectType imageObject = imageDocument.getAttachment().getEmbeddedDocumentBinaryObject();
        Resource imageResource = new ByteArrayResource(imageObject.getValue());

        logger.info("Downloading image with Id " + imageId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageObject.getMimeCode()))
                .body(imageResource);
    }

    @ApiOperation(value = "Delete company image")
    @RequestMapping(value = "/image/{imageId}", produces = {"application/json"}, method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteImage(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long imageId) throws IOException {

        logger.info("Deleting image with Id " + imageId);

        UaaUser user = identityUtils.getUserfromBearer(bearer);
        PartyType company = identityUtils.getCompanyOfUser(user).orElseThrow(CompanyNotFoundException::new);

        // remove from list in party
        if (company.getDocumentReference().stream().anyMatch(dr -> imageId.equals(dr.getHjid())) == false)
            throw new DocumentNotFoundException("No associated document found.");
        List<DocumentReferenceType> updatedList = company.getDocumentReference().stream()
                .filter(dr -> imageId.equals(dr.getHjid()) == false)
                .collect(Collectors.toList());
        company.setDocumentReference(updatedList);
        partyRepository.save(company);

        // delete object
        if (documentReferenceRepository.exists(imageId) == false)
            throw new DocumentNotFoundException("No document for Id found.");
        documentReferenceRepository.delete(imageId);

        return ResponseEntity.ok().build();
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

    @ApiOperation(value = "", notes = "Get profile completeness of company.", response = PartyType.class)
    @RequestMapping(value = "/{partyId}/completeness", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getProfileCompleteness(
            @ApiParam(value = "Id of party to retrieve profile completeness.", required = true) @PathVariable Long partyId
    ) {
        // search relevant parties
        List<PartyType> parties = partyRepository.findByHjid(partyId);

        // check if party was found
        if (parties.isEmpty()) {
            logger.info("Requested party with Id {} not found", partyId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType party = parties.get(0);
        QualifyingPartyType qualifyingParty = qualifyingPartyRepository.findByParty(party).stream().findFirst().orElse(null);

        CompanySettings companySettings = UblAdapter.adaptCompanySettings(party, qualifyingParty);

        // compute completeness factors
        Double detailsCompleteness = IdentityUtils.computeDetailsCompleteness(companySettings.getDetails());
        Double descriptionCompleteness = IdentityUtils.computeDescriptionCompleteness(companySettings.getDescription());
        Double certificateCompleteness = IdentityUtils.computeCertificateCompleteness(party);
        Double tradeCompleteness = IdentityUtils.computeTradeCompleteness(companySettings.getTradeDetails());
        Double overallCompleteness = (detailsCompleteness + descriptionCompleteness + certificateCompleteness + tradeCompleteness) / 4.0;

        List<QualityIndicatorType> qualityIndicators = new ArrayList<>();
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(PROFILE_COMPLETENESS, overallCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_GENERAL_DETAILS, detailsCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_DESCRIPTION, descriptionCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_CERTIFICATE_DETAILS, certificateCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_TRADE_DETAILS, overallCompleteness));
        PartyType completenessParty = new PartyType();
        completenessParty.setQualityIndicator(qualityIndicators);
        completenessParty.setID(party.getID());

        logger.debug("Returning completeness of party with Id {0}", party.getHjid());
        return new ResponseEntity<>(completenessParty, HttpStatus.OK);
    }

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "company not found")
    private static class CompanyNotFoundException extends RuntimeException {
    }

    @ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "File size exceeds limit")
    private static class FileTooLargeException extends RuntimeException {
    }

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "document not found")
    private static class DocumentNotFoundException extends RuntimeException {
        DocumentNotFoundException() {
        }

        DocumentNotFoundException(String message) {
            super(message);
        }
    }

    private void enrichImageMetadata(PartyType party) {
        // fetch only identifiers of images in order to avoid fetch of entire binary files
        List<DocumentReferenceType> imageDocuments = party.getDocumentReference().stream()
                .filter(d -> DOCUMENT_TYPE_COMPANY_LOGO.equals(d.getDocumentType()) || DOCUMENT_TYPE_COMPANY_PHOTO.equals(d.getDocumentType()))
                .collect(Collectors.toList());
        party.getDocumentReference().removeAll(imageDocuments);
        List<DocumentReferenceType> logos = partyRepository.findDocumentIds(party.getHjid(), DOCUMENT_TYPE_COMPANY_LOGO).stream()
                .map(id -> shallowDocumentReference(id, DOCUMENT_TYPE_COMPANY_LOGO))
                .collect(Collectors.toList());
        party.getDocumentReference().addAll(logos);
        List<DocumentReferenceType> images = partyRepository.findDocumentIds(party.getHjid(), DOCUMENT_TYPE_COMPANY_PHOTO).stream()
                .map(id -> shallowDocumentReference(id, DOCUMENT_TYPE_COMPANY_PHOTO))
                .collect(Collectors.toList());
        party.getDocumentReference().addAll(images);
    }

    private static DocumentReferenceType shallowDocumentReference(BigInteger identifier, String documentType) {
        DocumentReferenceType documentReference = new DocumentReferenceType();
        documentReference.setID(identifier.toString());
        documentReference.setHjid(identifier.longValue());
        documentReference.setDocumentType(documentType);
        return documentReference;
    }
}
