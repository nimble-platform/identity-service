package eu.nimble.core.infrastructure.identity.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.nimble.core.infrastructure.identity.clients.IndexingClient;
import eu.nimble.core.infrastructure.identity.clients.IndexingClientController;
import eu.nimble.core.infrastructure.identity.entity.CompanyDetailsUpdates;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyDetails;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanySettings;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.messaging.KafkaSender;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.service.AdminService;
import eu.nimble.core.infrastructure.identity.service.CertificateService;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.utils.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.collections.CollectionUtils;
import org.keycloak.representations.idm.UserRepresentation;
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
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.*;
import static eu.nimble.core.infrastructure.identity.utils.UblAdapter.*;
import static eu.nimble.service.model.ubl.extension.QualityIndicatorParameter.*;
import static eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@RestController
@RequestMapping("/company-settings")
@SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "FieldCanBeLocal"})
@Api(value = "company-settings", description = "API for handling settings of companies.")
public class CompanySettingsController {

    private final Long MAX_IMAGE_SIZE = 10 * 1024L * 1024L; // in bytes

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
    private IdentityService identityService;

    @Autowired
    private CertificateService certificateService;

    private BinaryContentService binaryContentService = new BinaryContentService();

    @Autowired
    private IndexingClientController indexingController;

    @Autowired
    private AdminService adminService;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private KafkaSender kafkaSender;

    @ApiOperation(value = "Retrieve company settings", response = CompanySettings.class)
    @RequestMapping(value = "/{companyID}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<CompanySettings> getSettings(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long companyID) {

        // search relevant parties
        PartyType company = partyRepository.findByHjid(companyID).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        Optional<QualifyingPartyType> qualifyingPartyOptional = qualifyingPartyRepository.findByParty(company).stream().findFirst();

        logger.debug("Returning requested settings for party with Id {}", company.getHjid());

        // pre fetch image metadata without binaries
        enrichImageMetadata(company);

        CompanySettings settings = UblAdapter.adaptCompanySettings(company, qualifyingPartyOptional.orElse(null));
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @ApiOperation(value = "Change company settings", response = CompanySettings.class)
    @RequestMapping(value = "/{companyID}", consumes = {"application/json"}, method = RequestMethod.PUT)
    ResponseEntity<?> setSettings(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company to change settings from.", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Settings to update.", required = true) @RequestBody CompanySettings newSettings)throws IOException{

        if (identityService.hasAnyRole(bearer,COMPANY_ADMIN,LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE, PUBLISHER) == false)
            return new ResponseEntity<>("Only legal representatives, company admin or platform managers are allowed add images", HttpStatus.FORBIDDEN);
        // retrieve party and qualifying party
        PartyType existingCompany = partyRepository.findByHjid(companyID).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        logger.debug("Changing settings for party with Id {}", existingCompany.getHjid());
        QualifyingPartyType qualifyingPartyType = qualifyingPartyRepository.findByParty(existingCompany).stream().findFirst().orElse(null);

        // create company details using the party and qualifying party
        CompanyDetails existingCompanyDetails = UblAdapter.adaptCompanyDetails(existingCompany,qualifyingPartyType);
        // update party and qualifying party
        existingCompany = UblAdapter.adaptCompanySettings(newSettings, null, existingCompany);

        QualifyingPartyType qualifyingParty = UblAdapter.adaptQualifyingParty(newSettings, existingCompany, qualifyingPartyType);
        qualifyingPartyRepository.save(qualifyingParty);

        // set preferred product categories
        List<CodeType> preferredProductCategories = UblAdapter.adaptProductCategories(newSettings.getPreferredProductCategories());
        existingCompany.getPreferredItemClassificationCode().clear();
        existingCompany.getPreferredItemClassificationCode().addAll(preferredProductCategories);

        // set recently used product categories
        List<CodeType> recentlyUsedProductCategories = UblAdapter.adaptProductCategories(newSettings.getRecentlyUsedProductCategories());
        existingCompany.getMostRecentItemsClassificationCode().clear();
        existingCompany.getMostRecentItemsClassificationCode().addAll(recentlyUsedProductCategories);

        partyRepository.save(existingCompany);

        eu.nimble.service.model.solr.party.PartyType indexedParty =  indexingController.getNimbleIndexClient().getParty(existingCompany.getHjid().toString(),bearer);
        //indexing the new company in the indexing service
        eu.nimble.service.model.solr.party.PartyType party = DataModelUtils.toIndexParty(existingCompany,qualifyingParty);
        if (indexedParty != null && indexedParty.getVerified()) {
            party.setVerified(true);
        }

        List<IndexingClient> indexingClients = indexingController.getClients();
        for(IndexingClient indexingClient : indexingClients){
            indexingClient.setParty(party,bearer);
        }

        newSettings = adaptCompanySettings(existingCompany, qualifyingParty);

        // if the company data is updated by the users which are not platform managers, let platform managers know about that change
        if(!identityService.hasAnyRole(bearer,PLATFORM_MANAGER)){
            // get the updates on company details
            CompanyDetailsUpdates companyDetailsUpdates = this.getCompanyDetailsUpdates(existingCompanyDetails,newSettings.getDetails());
            // check whether company data is updated
            if(companyDetailsUpdates.areCompanyDetailsUpdated()){
                PersonType person = identityService.getUserfromBearer(bearer).getUBLPerson();
                this.informPlatformManagerAboutCompanyDataUpdates(person,existingCompany,companyDetailsUpdates);
            }
        }
        return new ResponseEntity<>(newSettings, HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Upload company image")
    @RequestMapping(value = "/{companyID}/image", produces = {"application/json"}, method = RequestMethod.POST)
    public ResponseEntity<?> uploadImage(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company", required = true) @PathVariable Long companyID,
            @RequestParam(value = "isLogo", defaultValue = "false") String isLogo,
            @RequestParam(value = "file") MultipartFile imageFile) throws IOException {

        if (identityService.hasAnyRole(bearer,COMPANY_ADMIN,LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives, company admin or platform managers are allowed add images", HttpStatus.FORBIDDEN);

        if (imageFile.getSize() > MAX_IMAGE_SIZE)
            throw new ControllerUtils.FileTooLargeException();

        PartyType company = getCompanySecure(companyID, bearer);

        logger.info("Storing image for company with ID " + UblAdapter.adaptPartyIdentifier(company));

        Boolean logoFlag = "true".equals(isLogo);

        // scale image
        byte[] scaledImage = ImageUtils.scaleImage(imageFile.getBytes(), false, imageFile.getContentType());

        // store the original object in separate database
        BinaryObjectType binaryObject = new BinaryObjectType();
        binaryObject.setValue(scaledImage);
        binaryObject.setMimeCode(imageFile.getContentType());
        binaryObject.setFileName(imageFile.getOriginalFilename());
        binaryObject = binaryContentService.createContent(binaryObject);
        binaryObject.setValue(null); // reset value so it is not stored in database

        DocumentReferenceType imageDocument = UblAdapter.adaptCompanyPhoto(binaryObject, logoFlag);
        documentReferenceRepository.save(imageDocument);

        company.getDocumentReference().add(imageDocument);
        partyRepository.save(company);

        imageDocument.setID(imageDocument.getHjid().toString());
        imageDocument.getAttachment().getEmbeddedDocumentBinaryObject().setUri(null); // reset uri (images are handled differently)

        //indexing logo image uri for the existing party
        eu.nimble.service.model.solr.party.PartyType indexParty =  indexingController.getNimbleIndexClient().getParty(company.getHjid().toString(),bearer);
        indexParty.setLogoId(imageDocument.getID());

        List<IndexingClient> indexingClients = indexingController.getClients();
        for(IndexingClient indexingClient : indexingClients){
            indexingClient.setParty(indexParty,bearer);
        }

        return ResponseEntity.ok(imageDocument);
    }

    @ApiOperation(value = "Download company image")
    @RequestMapping(value = "/image/{imageId}", produces = {"application/json"}, method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadImage(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long imageId) {

        // collect image resource
        DocumentReferenceType imageDocument = documentReferenceRepository.findOne(imageId);
        if (imageDocument == null)
            throw new ControllerUtils.DocumentNotFoundException();

        String uri = imageDocument.getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        BinaryObjectType imageObject = binaryContentService.retrieveContent(uri);
        Resource imageResource = new ByteArrayResource(imageObject.getValue());

        logger.info("Downloading image with Id " + imageId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageObject.getMimeCode()))
                .body(imageResource);
    }

    @ApiOperation(value = "Delete company image")
    @RequestMapping(value = "/{companyID}/image/{imageId}", produces = {"application/json"}, method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteImage(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company owning the image", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Id of image to delete", required = true) @PathVariable Long imageId) throws IOException {

        if (identityService.hasAnyRole(bearer, COMPANY_ADMIN,LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives or platform managers are allowed to delete images", HttpStatus.FORBIDDEN);

        logger.info("Deleting image with Id " + imageId);

        PartyType company = getCompanySecure(companyID, bearer);

        if (company.getDocumentReference().stream().anyMatch(dr -> imageId.equals(dr.getHjid())) == false)
            throw new ControllerUtils.DocumentNotFoundException("No associated document found.");

        if (documentReferenceRepository.exists(imageId) == false)
            throw new ControllerUtils.DocumentNotFoundException("No document for Id found.");

        // delete binary content
        DocumentReferenceType imageDocument = documentReferenceRepository.findOne(imageId);
        String uri = imageDocument.getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        binaryContentService.deleteContentIdentity(uri);

        // delete document of company
        documentReferenceRepository.delete(imageDocument);

        // remove from list in party
        Optional<DocumentReferenceType> toDelete = company.getDocumentReference().stream()
                .filter(dr -> imageId.equals(dr.getHjid()))
                .findFirst();
        if (toDelete.isPresent()) {
            company.getDocumentReference().remove(toDelete.get());
            partyRepository.save(company);
        }

        //removing logo image id from the indexed the party
        eu.nimble.service.model.solr.party.PartyType indexParty =  indexingController.getNimbleIndexClient().getParty(company.getHjid().toString(),bearer);
        indexParty.setLogoId("");
        List<IndexingClient> indexingClients = indexingController.getClients();
        for(IndexingClient indexingClient : indexingClients){
            indexingClient.setParty(indexParty,bearer);
        }

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Certificate upload")
    @PostMapping("/{companyID}/certificate")
    public ResponseEntity<?> uploadCertificate(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company owning the certificate", required = true) @PathVariable Long companyID,
            @RequestParam("file") MultipartFile certFile,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam("langId") String languageId,
            @RequestParam("certID") String certID
    ) throws IOException {

        if (identityService.hasAnyRole(bearer, LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives or platform managers are allowed to upload certificates", HttpStatus.FORBIDDEN);

        PartyType company = getCompanySecure(companyID, bearer);

        if(!certID.equals("null")){
            Long certId = Long.parseLong(certID);
            // delete binary content
            CertificateType certificate = certificateRepository.findOne(certId);
            String uri = certificate.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri();
            binaryContentService.deleteContentIdentity(uri);

            // delete certificate
            certificateRepository.delete(certificate);

            // update list of certificates
            Optional<CertificateType> toDelete = company.getCertificate().stream()
                    .filter(c -> c.getHjid() != null)
                    .filter(c -> c.getHjid().equals(certId))
                    .findFirst();
            if (toDelete.isPresent()) {
                company.getCertificate().remove(toDelete.get());
                partyRepository.save(company);
            }
        }

        BinaryObjectType certificateBinary = new BinaryObjectType();
        // since the file content is Base64 encoded, decode it before saving
        certificateBinary.setValue(Base64.getDecoder().decode(certFile.getBytes()));
        certificateBinary.setFileName(certFile.getOriginalFilename());
        certificateBinary.setMimeCode(certFile.getContentType());
        certificateBinary.setLanguageID(languageId);
        certificateBinary = binaryContentService.createContent(certificateBinary);
        certificateBinary.setValue(null); // reset value so it is not stored in database

        // create new certificate
        CertificateType certificate = UblAdapter.adaptCertificate(certificateBinary, name, type, description);

        // update and store company
        company.getCertificate().add(certificate);
        partyRepository.save(company);

        return ResponseEntity.ok(certificate);
    }

    @ApiOperation(value = "Certificate download")
    @RequestMapping(value = "/certificate/{certificateId}", method = RequestMethod.GET)
    ResponseEntity<?> downloadCertificate(@ApiParam(value = "Id of certificate.", required = true) @PathVariable Long certificateId) {

        CertificateType certificateType = certificateService.queryCertificate(certificateId);
        if (certificateType == null)
            return ResponseEntity.notFound().build();

        String uri = certificateType.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        BinaryObjectType certFile = binaryContentService.retrieveContent(uri);
        Resource certResource = new ByteArrayResource(certFile.getValue());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(certFile.getMimeCode()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certFile.getFileName() + "\"")
                .body(certResource);
    }

    @ApiOperation(value = "Certificate download")
    @RequestMapping(value = "/certificate/{certificateId}/object", method = RequestMethod.GET)
    ResponseEntity<CertificateType> downloadCertificateObject(@ApiParam(value = "Id of certificate.", required = true) @PathVariable Long certificateId) {

        CertificateType certificateType = certificateService.queryCertificate(certificateId);
        if (certificateType == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        return ResponseEntity.ok().body(certificateType);
    }

    @ApiOperation(value = "Certificate deletion")
    @RequestMapping(value = "/{companyID}/certificate/{certificateId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteCertificate(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company owning the certificate", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Id of certificate.", required = true) @PathVariable Long certificateId) throws IOException {

        if (identityService.hasAnyRole(bearer, LEGAL_REPRESENTATIVE, PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only legal representatives or platform managers are allowed to delete images", HttpStatus.FORBIDDEN);

        PartyType company = getCompanySecure(companyID, bearer);

        if (certificateRepository.exists(certificateId) == false)
            throw new ControllerUtils.DocumentNotFoundException("No certificate for Id found.");

        // delete binary content
        CertificateType certificate = certificateRepository.findOne(certificateId);
        String uri = certificate.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri();
        binaryContentService.deleteContentIdentity(uri);

        // delete certificate
        certificateRepository.delete(certificate);

        // update list of certificates
        Optional<CertificateType> toDelete = company.getCertificate().stream()
                .filter(c -> c.getHjid() != null)
                .filter(c -> c.getHjid().equals(certificateId))
                .findFirst();
        if (toDelete.isPresent()) {
            company.getCertificate().remove(toDelete.get());
            partyRepository.save(company);
        }

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Update negotiation settings")
    @RequestMapping(value = "/{companyID}/negotiation", method = RequestMethod.PUT)
    ResponseEntity<?> updateNegotiationSettings(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company owning the certificate", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Settings to update.", required = true) @RequestBody NegotiationSettings newSettings) throws IOException {

        if (identityService.hasAnyRole(bearer, LEGAL_REPRESENTATIVE, PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only legal representatives or platform managers are allowed to update settings", HttpStatus.FORBIDDEN);

        PartyType company = getCompanySecure(companyID, bearer);

        // retrieve existing negotiation settings
        NegotiationSettings existingSettings = findOrCreateNegotiationSettings(company);
        // check whether the available process ids are updated for the company
        boolean isProcessIdListUpdated = false;
        if(!CollectionUtils.isEqualCollection(existingSettings.getCompany().getProcessID(),newSettings.getCompany().getProcessID())){
            isProcessIdListUpdated = true;
        }
        // update settings
        existingSettings.update(newSettings);
        existingSettings = negotiationSettingsRepository.save(existingSettings);

        // when the available process id list is updated for the company,
        // we need to recalculate the company rating since the available sub-ratings depend on the selected process ids
        // broadcast the change on ratings
        if(isProcessIdListUpdated){
            kafkaSender.broadcastRatingsUpdate(String.valueOf(companyID),bearer);
        }
        logger.info("Updated negotiation settings {} for company {}", existingSettings.getId(), UblAdapter.adaptPartyIdentifier(company));

//        //indexing the updated company in the indexing service
//        eu.nimble.service.model.solr.party.PartyType party = DataModelUtils.toIndexParty(company);
//        indexingClient.setParty(party);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Get negotiation settings", response = NegotiationSettings.class)
    @RequestMapping(value = "/{companyID}/negotiation/", method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<?> getNegotiationSettings(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long companyID) {

        PartyType company = partyRepository.findByHjid(companyID).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        NegotiationSettings negotiationSettings = findOrCreateNegotiationSettings(company);

        try {
            String serializedNegotiationSettings = JsonSerializationUtility.getObjectMapper().writeValueAsString(negotiationSettings);
            logger.info("Fetched negotiation settings {} for company {}", negotiationSettings.getId(), UblAdapter.adaptPartyIdentifier(company));
            return new ResponseEntity<>(serializedNegotiationSettings, HttpStatus.OK);

        } catch (JsonProcessingException e) {
            return createResponseEntityAndLog(String.format("Serialization error for negotiation settings: %s for company %s", negotiationSettings.getId(),UblAdapter.adaptPartyIdentifier(company)), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "", notes = "Get profile completeness of company.", response = PartyType.class)
    @RequestMapping(value = "/{companyID}/completeness", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getProfileCompleteness(
            @ApiParam(value = "Id of party to retrieve profile completeness.", required = true) @PathVariable Long companyID
    ) {
        // search relevant parties
        PartyType company = partyRepository.findByHjid(companyID).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        QualifyingPartyType qualifyingParty = qualifyingPartyRepository.findByParty(company).stream().findFirst().orElse(null);
        NegotiationSettings negotiationSettings = negotiationSettingsRepository.findByCompany(company).stream().findFirst().orElse(null);

        CompanySettings companySettings = UblAdapter.adaptCompanySettings(company, qualifyingParty);

        // compute completeness factors
        Double detailsCompleteness = IdentityService.computeDetailsCompleteness(companySettings.getDetails()) * 3;
        Double descriptionCompleteness = IdentityService.computeDescriptionCompleteness(companySettings.getDescription()) * 2;
        Double deliveryAddressCompleteness = IdentityService.computeDeliveryAddressCompleteness(company) * 2;
        Double certificateCompleteness = IdentityService.computeCertificateCompleteness(company) * 1.5;
        Double tradeCompleteness = IdentityService.computeTradeCompleteness(negotiationSettings);
        Double nonMandatoryDataCompleteness = IdentityService.computeAdditionalDataCompleteness(company, companySettings.getTradeDetails(), companySettings.getDescription()) * 1.5;

        Double overallCompleteness = (detailsCompleteness + descriptionCompleteness + certificateCompleteness +
                 deliveryAddressCompleteness+ nonMandatoryDataCompleteness) / 10.0;

        List<QualityIndicatorType> qualityIndicators = new ArrayList<>();
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(PROFILE_COMPLETENESS, overallCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_GENERAL_DETAILS, detailsCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_DESCRIPTION, descriptionCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_CERTIFICATE_DETAILS, certificateCompleteness));
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(COMPLETENESS_OF_COMPANY_TRADE_DETAILS, tradeCompleteness));
        PartyType completenessParty = new PartyType();
        completenessParty.setQualityIndicator(qualityIndicators);
        UblUtils.setID(completenessParty, UblAdapter.adaptPartyIdentifier(company));

        logger.debug("Returning completeness of party with Id {0}", company.getHjid());
        return new ResponseEntity<>(completenessParty, HttpStatus.OK);
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

    private PartyType getCompanySecure(Long companyID, String bearer) throws IOException {
        PartyType company = partyRepository.findByHjid(companyID).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        // check if legal representative is from same company
        UaaUser user = identityService.getUserfromBearer(bearer);
        PartyType companyFromBearer = identityService.getCompanyOfUser(user).orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        if( identityService.hasAnyRole(bearer, PLATFORM_MANAGER) == false && companyFromBearer.getHjid().equals(companyID) == false)
            throw new ControllerUtils.UnauthorisedAccess();

        return company;
    }

    @RequestMapping(value = "/vat/{vat}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> getVatInfo(@PathVariable String vat) throws UnirestException {
        logger.debug("Querying VAT info for " + vat);
        HttpResponse<JsonNode> response = Unirest.get("https://taxapi.io/api/v1/vat?vat_number=" + vat).asJson();
        return new ResponseEntity<>(response.getBody().toString(), HttpStatus.OK);
    }

    /**
     * admin endpoint to reindex all valid parties in indexing service (for platform manager/admin purposes only)
     * @return 200 OK
     * @throws UnirestException
     */
    @RequestMapping(value = "/reindexParties", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> reindexAllCompanies(@RequestHeader(value = "Authorization") String bearer) throws IOException{
        if (identityService.hasAnyRole(bearer, PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only platform managers are allowed to reindex all companies", HttpStatus.FORBIDDEN);
        logger.debug("indexing all companies. ");
        //adding verified and unverified companies with valid userRoles
        List<PartyType> verifiedCompanies = adminService.queryCompanies(AdminService.CompanyState.VERIFIED);
        List<PartyType> unVerifiedCompanies = adminService.queryCompanies(AdminService.CompanyState.UNVERIFIED);

        for (PartyType party : verifiedCompanies) {
            Optional<QualifyingPartyType> qualifyingPartyTypeOptional = qualifyingPartyRepository.findByParty(party).stream().findFirst();
            if(qualifyingPartyTypeOptional.isPresent()){
                QualifyingPartyType qualifyingPartyType = qualifyingPartyTypeOptional.get();
                eu.nimble.service.model.solr.party.PartyType newParty = DataModelUtils.toIndexParty(party,qualifyingPartyType);
                newParty.setVerified(true);
                logger.info("Indexing verified party from database to index : " + newParty.getId() + " legalName : " + newParty.getLegalName());
                List<IndexingClient> indexingClients = indexingController.getClients();
                for(IndexingClient indexingClient : indexingClients) {
                    indexingClient.setParty(newParty,bearer);
                }
            }
        }
        for (PartyType party : unVerifiedCompanies) {
            Optional<QualifyingPartyType> qualifyingPartyTypeOptional = qualifyingPartyRepository.findByParty(party).stream().findFirst();
            if(qualifyingPartyTypeOptional.isPresent()){
                QualifyingPartyType qualifyingPartyType = qualifyingPartyTypeOptional.get();
                eu.nimble.service.model.solr.party.PartyType newParty = DataModelUtils.toIndexParty(party,qualifyingPartyType);
                logger.info("Indexing unverified party from database to index : " + newParty.getId() + " legalName : " + newParty.getLegalName());
                List<IndexingClient> indexingClients = indexingController.getClients();
                for (IndexingClient indexingClient : indexingClients) {
                    indexingClient.setParty(newParty, bearer);
                }
            }
        }

        return new ResponseEntity<>("Completed indexing all companies", HttpStatus.OK);
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

    private void informPlatformManagerAboutCompanyDataUpdates(PersonType representative, PartyType company, CompanyDetailsUpdates companyDetailsUpdates) {
        List<UserRepresentation> managers = keycloakAdmin.getPlatformManagers();
        List<String> emails = managers.stream().map(UserRepresentation::getEmail).collect(Collectors.toList());

        emailService.notifyPlatformManagersCompanyDataUpdates(emails, representative, company, companyDetailsUpdates,executionContext.getLanguageId());
    }

    /**
     * Returns the updated fields of company {{@link CompanyDetailsUpdates}} based on the old and new company details
     * @param oldCompanyDetails the old company details
     * @param newCompanyDetails the new company details
     * @return the updated fields of company as {{@link CompanyDetailsUpdates}}
     * */
    private CompanyDetailsUpdates getCompanyDetailsUpdates(CompanyDetails oldCompanyDetails, CompanyDetails newCompanyDetails){
        CompanyDetailsUpdates companyDetailsUpdates = new CompanyDetailsUpdates();
        // vat number
        if(!StringUtils.equals(oldCompanyDetails.getVatNumber(),newCompanyDetails.getVatNumber()))
            companyDetailsUpdates.setVatNumber(newCompanyDetails.getVatNumber());
        // verification info
        if(!StringUtils.equals(oldCompanyDetails.getVerificationInformation(),newCompanyDetails.getVerificationInformation()))
            companyDetailsUpdates.setVerificationInformation(newCompanyDetails.getVerificationInformation());
        // business type
        if(!StringUtils.equals(oldCompanyDetails.getBusinessType(),newCompanyDetails.getBusinessType()))
            companyDetailsUpdates.setBusinessType(newCompanyDetails.getBusinessType());
        // year of foundation
        if(!Objects.equals(oldCompanyDetails.getYearOfCompanyRegistration(),newCompanyDetails.getYearOfCompanyRegistration()))
            companyDetailsUpdates.setYearOfCompanyRegistration(newCompanyDetails.getYearOfCompanyRegistration());
        // brand name
        if(!Objects.equals(oldCompanyDetails.getBrandName(),newCompanyDetails.getBrandName()))
            companyDetailsUpdates.setBrandName(newCompanyDetails.getBrandName());
        // legal name
        if(!Objects.equals(oldCompanyDetails.getLegalName(),newCompanyDetails.getLegalName()))
            companyDetailsUpdates.setLegalName(newCompanyDetails.getLegalName());
        // industry sectors
        if(!Objects.equals(oldCompanyDetails.getIndustrySectors(),newCompanyDetails.getIndustrySectors()))
            companyDetailsUpdates.setIndustrySectors(newCompanyDetails.getIndustrySectors());
        // business keywords
        if(!Objects.equals(oldCompanyDetails.getBusinessKeywords(),newCompanyDetails.getBusinessKeywords()))
            companyDetailsUpdates.setBusinessKeywords(newCompanyDetails.getBusinessKeywords());
        // address
        if(!Objects.equals(oldCompanyDetails.getAddress(), newCompanyDetails.getAddress())){
            companyDetailsUpdates.setAddress(newCompanyDetails.getAddress());
        }
        return companyDetailsUpdates;
    }
}
