package eu.nimble.core.infrastructure.identity.controller.frontend;

import com.sun.org.apache.xpath.internal.operations.Neg;
import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanySettings;
import eu.nimble.core.infrastructure.identity.repository.CertificateRepository;
import eu.nimble.core.infrastructure.identity.repository.NegotiationSettingsRepository;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
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

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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
@RequestMapping("/company-settings")
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


        NegotiationSettings negotiationSettings = findOrCreateNegotiationSettings(party.get());
        CompanySettings settings = UblAdapter.adaptCompanySettings(party.get(), negotiationSettings);
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @ApiOperation(value = "Change company settings")
    @RequestMapping(value = "/{companyID}", consumes = {"application/json"}, method = RequestMethod.PUT)
    ResponseEntity<CompanySettings> setSettings(
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
        party.setDeliveryTerms(deliveryTerms);

        // set payment means
        List<PaymentMeansType> paymentMeans = newSettings.getPaymentMeans().stream().map(UblAdapter::adaptPaymentMeans).collect(Collectors.toList());
        party.setPaymentMeans(paymentMeans);

        // set address
        AddressType companyAddress = UblAdapter.adaptAddress(newSettings.getAddress());
        party.setPostalAddress(companyAddress);

        // set default PPAP level
        int ppapLevel = newSettings.getPpapCompatibilityLevel() != null ? newSettings.getPpapCompatibilityLevel() : 0;
        party.setPpapCompatibilityLevel(BigDecimal.valueOf(ppapLevel));

        // set preferred product categories
        List<CodeType> preferredProductCategories = UblAdapter.adaptPreferredCategories(newSettings.getPreferredProductCategories());
        party.setPreferredItemClassificationCode(preferredProductCategories);

        // set miscellaneous
        party.setWebsiteURI(newSettings.getWebsite());
        List<PartyTaxSchemeType> partyTaxSchemes = new ArrayList<>();
        partyTaxSchemes.add(UblAdapter.adaptTaxSchema(newSettings.getVatNumber()));
        party.setPartyTaxScheme(partyTaxSchemes);
        List<QualityIndicatorType> qualityIndicators = new ArrayList<>();
        qualityIndicators.add(UblAdapter.adaptQualityIndicator(newSettings.getVerificationInformation()));
        party.setQualityIndicator(qualityIndicators);

        partyRepository.save(party);

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
        Optional<PartyType> companyOpt = identityUtils.getCompanyOfUser(user);
        if (companyOpt.isPresent() == false)
            ResponseEntity.notFound().build();
        PartyType company = companyOpt.get();

        // create new certificate
        CertificateType certificate = UblAdapter.adaptCertificate(file, name, type, company);

        // update and store company
        company.getCertificate().add(certificate);
        partyRepository.save(company);

        return ResponseEntity.ok().body(company.getCertificate());
    }


    @ApiOperation(value = "Certificate download")
    @RequestMapping(value = "/certificate/{certificateId}", method = RequestMethod.GET)
    ResponseEntity<?> downloadCertificate(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of certificate.", required = true) @PathVariable Long certificateId) {

        // ToDo: check if company is associated with user

        CertificateType certificateType = certificateRepository.findOne(certificateId);
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
        Optional<PartyType> companyOpt = identityUtils.getCompanyOfUser(user);
        if (companyOpt.isPresent() == false)
            ResponseEntity.notFound().build();
        PartyType company = companyOpt.get();

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
        Optional<PartyType> companyOpt = identityUtils.getCompanyOfUser(user);
        if (companyOpt.isPresent() == false)
            ResponseEntity.notFound().build();
        PartyType company = companyOpt.get();

        // query existing settings
        NegotiationSettings existingSettings = findOrCreateNegotiationSettings(company);

        // update settings
        newSettings.setCompany(company);
        newSettings.setId(existingSettings.getId());
        negotiationSettingsRepository.save(newSettings);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Get negotiation settings", response = NegotiationSettings.class)
    @RequestMapping(value = "/negotiation", method = RequestMethod.GET, produces = "application/json")
    ResponseEntity<?> getNegotiationSettings(
            @RequestHeader(value = "Authorization") String bearer) throws IOException {

        // find company
        UaaUser user = identityUtils.getUserfromBearer(bearer);
        if( user == null )
            return ResponseEntity.notFound().build();

        Optional<PartyType> companyOpt = identityUtils.getCompanyOfUser(user);
        if (companyOpt.isPresent() == false)
            return ResponseEntity.notFound().build();
        PartyType company = companyOpt.get();

        NegotiationSettings negotiationSettings = findOrCreateNegotiationSettings(company);
        return ResponseEntity.ok().body(negotiationSettings);
    }

    private NegotiationSettings findOrCreateNegotiationSettings(PartyType company) {
        NegotiationSettings negotiationSettings = negotiationSettingsRepository.findOneByCompany(company);
        if (negotiationSettings == null) {
            negotiationSettings = new NegotiationSettings();
            negotiationSettings.setCompany(company);
            negotiationSettings = negotiationSettingsRepository.save(negotiationSettings);
        }
        return negotiationSettings;
    }
}
