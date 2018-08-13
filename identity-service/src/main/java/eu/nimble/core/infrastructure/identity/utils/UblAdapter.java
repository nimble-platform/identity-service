package eu.nimble.core.infrastructure.identity.utils;

import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@SuppressWarnings("WeakerAccess")
public class UblAdapter {

    public static CompanySettings adaptCompanySettings(PartyType party, NegotiationSettings negotiationSettings) {
        CompanySettings settings = new CompanySettings();
        settings.setName(party.getName());
        settings.setWebsite(party.getWebsiteURI());
        settings.setAddress(adaptAddress(party.getPostalAddress()));

        // set payment means
        if (party.getPurchaseTerms() != null) {
            List<PaymentMeans> paymentMeans = party.getPurchaseTerms().getPaymentMeans().stream()  // ToDo: improve for sales terms
                    .map(UblAdapter::adaptPaymentMeans)
                    .collect(Collectors.toList());
            settings.setPaymentMeans(paymentMeans);
        }

        // set delivery terms
        if (party.getPurchaseTerms() != null) {
            List<DeliveryTerms> deliveryTerms = party.getPurchaseTerms().getDeliveryTerms().stream()  // ToDo: improve for sales terms
                    .map(UblAdapter::adaptDeliveryTerms)
                    .collect(Collectors.toList());
            settings.setDeliveryTerms(deliveryTerms);
        }

        settings.setVerificationInformation(adaptQualityIndicator(party));
        settings.setVatNumber(adaptVatNumber(party));
        if (party.getPpapCompatibilityLevel() != null)
            settings.setPpapCompatibilityLevel(party.getPpapCompatibilityLevel().intValue());
        settings.setCertificates(UblAdapter.adaptCertificates(party.getCertificate()));

        // set preferred product categories
        Set<String> preferredProductCategories = party.getPreferredItemClassificationCode().stream()
                .map(CodeType::getValue)
                .collect(Collectors.toSet());
        settings.setPreferredProductCategories(preferredProductCategories);

        // set recently used product categories
        Set<String> recentlyUsedProductCategories = party.getMostRecentItemsClassificationCode().stream()
                .map(CodeType::getValue)
                .collect(Collectors.toSet());
        settings.setRecentlyUsedProductCategories(recentlyUsedProductCategories);

        List<String> industrySectors = party.getIndustrySector().stream().map(CodeType::getValue).collect(Collectors.toList());
        settings.setIndustrySectors(industrySectors);

        settings.setNegotiationSettings(negotiationSettings);

        return settings;
    }

    public static Address adaptAddress(AddressType ublAddress) {

        if (ublAddress == null)
            return new Address();

        Address dtoAddress = new Address();
        dtoAddress.setStreetName(ublAddress.getStreetName());
        dtoAddress.setBuildingNumber(ublAddress.getBuildingNumber());
        dtoAddress.setCityName(ublAddress.getCityName());
        dtoAddress.setPostalCode(ublAddress.getPostalZone());
        if (ublAddress.getCountry() != null)
            dtoAddress.setCountry(ublAddress.getCountry().getName());
        return dtoAddress;
    }

    public static AddressType adaptAddress(Address dtoAddress) {

        if (dtoAddress == null)
            return new AddressType();

        AddressType ublAddress = new AddressType();
        ublAddress.setStreetName(dtoAddress.getStreetName());
        ublAddress.setBuildingNumber(dtoAddress.getBuildingNumber());
        ublAddress.setCityName(dtoAddress.getCityName());
        ublAddress.setPostalZone(dtoAddress.getPostalCode());

        CountryType country = new CountryType();
        country.setName(dtoAddress.getCountry());
        ublAddress.setCountry(country);

        return ublAddress;
    }

    public static DeliveryTerms adaptDeliveryTerms(DeliveryTermsType ublDeliveryTerms) {

        if (ublDeliveryTerms == null)
            return new DeliveryTerms();

        DeliveryTerms dtoDeliveryTerms = new DeliveryTerms();
        dtoDeliveryTerms.setSpecialTerms(ublDeliveryTerms.getSpecialTerms());

        // adapt address
        if (ublDeliveryTerms.getDeliveryLocation() != null)
            dtoDeliveryTerms.setDeliveryAddress(adaptAddress(ublDeliveryTerms.getDeliveryLocation().getAddress()));

        // adapt delivery period
        if (ublDeliveryTerms.getEstimatedDeliveryPeriod() != null)
            if (ublDeliveryTerms.getEstimatedDeliveryPeriod().getDurationMeasure() != null)
                dtoDeliveryTerms.setEstimatedDeliveryTime(ublDeliveryTerms.getEstimatedDeliveryPeriod().getDurationMeasure().getValue().intValue());

        return dtoDeliveryTerms;
    }

    public static DeliveryTermsType adaptDeliveryTerms(DeliveryTerms dtoDeliveryTerms) {

        if (dtoDeliveryTerms == null)
            return new DeliveryTermsType();

        DeliveryTermsType ublDeliveryTerms = new DeliveryTermsType();
        ublDeliveryTerms.setSpecialTerms(dtoDeliveryTerms.getSpecialTerms());

        // adapt address
        AddressType deliveryAddress = adaptAddress(dtoDeliveryTerms.getDeliveryAddress());
        LocationType deliveryLocation = new LocationType();
        deliveryLocation.setAddress(deliveryAddress);
        ublDeliveryTerms.setDeliveryLocation(deliveryLocation);

        // adapt delivery time
        if (dtoDeliveryTerms.getEstimatedDeliveryTime() != null) {
            QuantityType deliveryTimeQuantity = new QuantityType();
            deliveryTimeQuantity.setValue(new BigDecimal(dtoDeliveryTerms.getEstimatedDeliveryTime()));
            deliveryTimeQuantity.setUnitCode("Days");
            PeriodType deliveryPeriod = new PeriodType();
            deliveryPeriod.setDurationMeasure(deliveryTimeQuantity);
            ublDeliveryTerms.setEstimatedDeliveryPeriod(deliveryPeriod);
        }

        return ublDeliveryTerms;
    }

    public static PaymentMeansType adaptPaymentMeans(PaymentMeans dtoPaymentMeans) {

        if (dtoPaymentMeans == null)
            return new PaymentMeansType();

        PaymentMeansType ublPaymentMeans = new PaymentMeansType();
        ublPaymentMeans.setInstructionNote(dtoPaymentMeans.getInstructionNote());
        return ublPaymentMeans;
    }

    public static PaymentMeans adaptPaymentMeans(PaymentMeansType ublPaymentMeans) {

        if (ublPaymentMeans == null)
            return new PaymentMeans();

        PaymentMeans dtoPaymentMeans = new PaymentMeans();
        dtoPaymentMeans.setInstructionNote(ublPaymentMeans.getInstructionNote());
        return dtoPaymentMeans;
    }

    public static FrontEndUser adaptUser(UaaUser uaaUser, List<PartyType> companies) {
        FrontEndUser frontEndUser = new FrontEndUser();
        frontEndUser.setUsername(uaaUser.getUsername());
        PersonType ublPerson = uaaUser.getUBLPerson();
        frontEndUser.setFirstname(ublPerson.getFirstName());
        frontEndUser.setLastname(ublPerson.getFamilyName());
        frontEndUser.setShowWelcomeInfo(uaaUser.getShowWelcomeInfo());
        if (ublPerson.getContact() != null)
            frontEndUser.setEmail(ublPerson.getContact().getElectronicMail());
        frontEndUser.setUserID(ublPerson.getHjid());

        // set company ids
        if (companies != null && companies.isEmpty() == false) {
            PartyType company = companies.get(0);
            frontEndUser.setCompanyID(company.getHjid().toString());
            frontEndUser.setCompanyName(company.getName());
        }

        return frontEndUser;
    }

    public static PersonType adaptPerson(FrontEndUser frontEndUser) {
        PersonType person = new PersonType();
        person.setFirstName(frontEndUser.getFirstname());
        person.setFamilyName(frontEndUser.getLastname());
//        adminPerson.setBirthDate(frontEndUser.getDateOfBirth()); // TODO: convert date
        person.setBirthplaceName(frontEndUser.getPlaceOBirth());
        ContactType contact = new ContactType();
        contact.setElectronicMail(frontEndUser.getEmail());
        contact.setTelephone(frontEndUser.getPhoneNumber());
        person.setContact(contact);
        return person;
    }

    public static PartyType adaptCompanyRegistration(CompanyRegistration registration, PersonType admin) {

        PartyType companyParty = new PartyType();

        // adapt VAT number
        companyParty.getPartyTaxScheme().add(adaptTaxSchema(registration.getVatNumber()));

        // adapt verification information
        QualityIndicatorType qualityIndicatorType = adaptQualityIndicator(registration.getVerificationInformation());
        companyParty.getQualityIndicator().add(qualityIndicatorType);

        companyParty.setWebsiteURI(registration.getWebsite());
        companyParty.setName(registration.getName());
        companyParty.getPerson().add(admin);
        companyParty.setPostalAddress(adaptAddress(registration.getAddress()));

        return companyParty;
    }

    public static QualityIndicatorType adaptQualityIndicator(String verificationInformation) {
        QualityIndicatorType qualityIndicatorType = new QualityIndicatorType();
        qualityIndicatorType.setQualityParameter(verificationInformation);
        return qualityIndicatorType;
    }

    public static String adaptQualityIndicator(PartyType party) {
        if (party == null)
            return null;
        Optional<QualityIndicatorType> verificationInformationOpt = party.getQualityIndicator().stream().findFirst();
        return verificationInformationOpt.map(QualityIndicatorType::getQualityParameter).orElse(null);
    }

    public static PartyTaxSchemeType adaptTaxSchema(String vatNumber) {
        CodeType codeType = new CodeType();
        codeType.setName("VAT");
        codeType.setValue(vatNumber);
        TaxSchemeType taxScheme = new TaxSchemeType();
        taxScheme.setTaxTypeCode(codeType);
        PartyTaxSchemeType partyTaxSchemeType = new PartyTaxSchemeType();
        partyTaxSchemeType.setTaxScheme(taxScheme);
        return partyTaxSchemeType;
    }

    public static CertificateType adaptCertificate(MultipartFile certFile, String name, String type, PartyType issuer) throws IOException {

        CodeType codeType = new CodeType();
        codeType.setName(name);

        BinaryObjectType certificateBinary = new BinaryObjectType();
        certificateBinary.setValue(certFile.getBytes());
        certificateBinary.setFileName(certFile.getOriginalFilename());
        certificateBinary.setMimeCode(certFile.getContentType());
        AttachmentType attachmentType = new AttachmentType();
        attachmentType.setEmbeddedDocumentBinaryObject(certificateBinary);
        DocumentReferenceType documentReferenceType = new DocumentReferenceType();
        documentReferenceType.setAttachment(attachmentType);

        CertificateType certificateType = new CertificateType();
//        certificateType.setIssuerParty(issuer);
        certificateType.setCertificateType(type);
        certificateType.setCertificateTypeCode(codeType);
        certificateType.getDocumentReference().add(documentReferenceType);

        return certificateType;
    }

    public static List<CompanyCertificate> adaptCertificates(List<CertificateType> certificateTypes) {
        return certificateTypes.stream()
                .map(certificateType ->
                        new CompanyCertificate(certificateType.getCertificateTypeCode().getName(),
                                certificateType.getCertificateType(),
                                certificateType.getHjid().toString()))
                .collect(Collectors.toList());
    }

    public static String adaptVatNumber(PartyType partyType) {

        if (partyType == null)
            return null;

        String vatNumber = null;
        Optional<PartyTaxSchemeType> partyTaxSchemeOpt = partyType.getPartyTaxScheme().stream().findFirst();
        if (partyTaxSchemeOpt.isPresent()) {
            PartyTaxSchemeType partyTaxScheme = partyTaxSchemeOpt.get();
            if (partyTaxScheme.getTaxScheme() != null && partyTaxScheme.getTaxScheme().getTaxTypeCode() != null)
                vatNumber = partyTaxScheme.getTaxScheme().getTaxTypeCode().getValue();
        }
        return vatNumber;
    }

    public static List<CodeType> adaptProductCategories(Set<String> categoryCodes) {
        return categoryCodes.stream()
                .map(category -> {
                    CodeType code = new CodeType();
                    code.setValue(category);
                    return code;
                }).collect(Collectors.toList());
    }

    public static List<CodeType> adaptIndustrySectors(List<String> industrySectors) {
        return industrySectors.stream()
                .map(sector -> {
                    CodeType code = new CodeType();
                    code.setValue(sector);
                    return code;
                }).collect(Collectors.toList());
    }
}
