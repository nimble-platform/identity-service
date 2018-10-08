package eu.nimble.core.infrastructure.identity.utils;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.extension.QualityIndicatorParameter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@SuppressWarnings("WeakerAccess")
public class UblAdapter {

    public static final String VAT_TAX_TYPE_CODE = "VAT";

    public static CompanySettingsV2 changeCompanySettings(PartyType party) {
        CompanySettingsV2 settings = new CompanySettingsV2();

        CompanyDescription companyDescription = adaptCompanyDescription(party, null);
        CompanyDetails companyDetails = adaptCompanyDetails(party, null);

        settings.setCompanyID(party.getID());
        settings.setDescription(companyDescription);
        settings.setDetails(companyDetails);

//        // set payment means
//        if (party.getPurchaseTerms() != null) {
//            List<PaymentMeans> paymentMeans = party.getPurchaseTerms().getPaymentMeans().stream()  // ToDo: improve for sales terms
//                    .map(UblAdapter::adaptPaymentMeans)
//                    .collect(Collectors.toList());
//            settings.setPaymentMeans(paymentMeans);
//
//
//        // set delivery terms
//        if (party.getPurchaseTerms() != null) {
//            List<DeliveryTerms> deliveryTerms = party.getPurchaseTerms().getDeliveryTerms().stream()  // ToDo: improve for sales terms
//                    .map(UblAdapter::adaptDeliveryTerms)
//                    .collect(Collectors.toList());
//            settings.setDeliveryTerms(deliveryTerms);
//        }
//
////        settings.setVerificationInformation(adaptQualityIndicator(party)); // ToDo: refactor!!!!!!
//        settings.setVatNumber(adaptVatNumber(party));
//        if (party.getPpapCompatibilityLevel() != null)
//            settings.setPpapCompatibilityLevel(party.getPpapCompatibilityLevel().intValue());
//        settings.setCertificates(UblAdapter.adaptCertificates(party.getCertificate()));

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
        settings.getDetails().setIndustrySectors(industrySectors);

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

    public static CompanyDetails adaptCompanyDetails(PartyType party, QualifyingPartyType qualifyingParty) {

        CompanyDetails companyDetails = new CompanyDetails();

        companyDetails.setCompanyLegalName(party.getName());
        companyDetails.setVatNumber(party.getPartyTaxScheme()
                .stream()
                .filter(scheme -> scheme.getTaxScheme().getTaxTypeCode().getValue().equals(VAT_TAX_TYPE_CODE))
                .map(scheme -> scheme.getTaxScheme().getTaxTypeCode().getValue())
                .findFirst().orElse(null));
        companyDetails.setAddress(adaptAddress(party.getPostalAddress()));

        if (qualifyingParty != null) {
            companyDetails.setVerificationInformation(qualifyingParty.getBusinessIdentityEvidenceID());
            companyDetails.setBusinessKeywords(qualifyingParty.getBusinessClassificationScheme().getDescription());
            companyDetails.setYearOfCompanyRegistration(qualifyingParty.getOperatingYearsQuantity().getValue().intValue());
        }

        return companyDetails;
    }

    public static CompanyDescription adaptCompanyDescription(PartyType party, QualifyingPartyType qualifyingPartyType) {

        CompanyDescription companyDescription = new CompanyDescription();
        companyDescription.setWebsite(party.getWebsiteURI());
        companyDescription.setSocialMediaList(party.getContact().getOtherCommunication()
                .stream()
                .map(CommunicationType::getValue)
                .collect(Collectors.toList()));

        if (qualifyingPartyType != null) {
            companyDescription.setCompanyStatement(qualifyingPartyType.getEconomicOperatorRole().getRoleDescription().get(0));
            // ToDo: add events
        }

        return companyDescription;
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

    public static PartyType adaptCompanyRegistration(CompanyRegistration registration, PersonType representative) {
        return changeCompanySettings(registration.getSettings(), representative, null);
    }

    public static PartyType changeCompanySettings(CompanySettingsV2 settings, PersonType representative, PartyType companyToChange) {

        if (companyToChange == null)
            companyToChange = new PartyType();

        // legal name
        companyToChange.setName(settings.getDetails().getCompanyLegalName());

        // VAT number
        companyToChange.getPartyTaxScheme().add(adaptTaxSchema(settings.getDetails().getVatNumber()));

        // postal address
        companyToChange.setPostalAddress(adaptAddress(settings.getDetails().getAddress()));

        // classification code
        companyToChange.setIndustryClassificationCode(adaptCodeType("IndustryClassificationCode", settings.getDetails().getBusinessType()));

        // website URL
        companyToChange.setWebsiteURI(settings.getDescription().getWebsite());

        // social media list
        ContactType socialMediaContact = new ContactType();
        socialMediaContact.setOtherCommunication(adaptSocialMediaList(settings.getDescription().getSocialMediaList()));
        companyToChange.setContact(socialMediaContact);

        // industry sectors
        List<CodeType> industrySectors = UblAdapter.adaptIndustrySectors(settings.getDetails().getIndustrySectors());
        companyToChange.setIndustrySector(industrySectors);

        // check if representative is not already in list
        if (representative != null) {
            boolean alreadyInList = companyToChange.getPerson()
                    .stream()
                    .anyMatch(personType -> personType.getContact().getElectronicMail().equals(representative.getContact().getElectronicMail()));
            if (alreadyInList == false)
                companyToChange.getPerson().add(representative);
        }

        // PPAP
        int ppapLevel = settings.getTradeDetails().getPpapCompatibilityLevel() != null ? settings.getTradeDetails().getPpapCompatibilityLevel() : 0;
        companyToChange.setPpapCompatibilityLevel(BigDecimal.valueOf(ppapLevel));

        return companyToChange;
    }

    public static QualifyingPartyType adaptQualifyingParty(CompanySettingsV2 settings, PartyType company) {

        QualifyingPartyType qualifyingParty = new QualifyingPartyType();

        // set verification info
        qualifyingParty.setBusinessClassificationEvidenceID(settings.getDetails().getVerificationInformation());

        // business keywords
        ClassificationSchemeType classificationScheme = new ClassificationSchemeType();
        classificationScheme.setDescription(new ArrayList<>(settings.getDetails().getBusinessKeywords()));
        qualifyingParty.setBusinessClassificationScheme(classificationScheme);

        // year of company registration
        QuantityType years = new QuantityType();
        years.setValue(new BigDecimal(settings.getDetails().getYearOfCompanyRegistration()));
        qualifyingParty.setOperatingYearsQuantity(years);

        // company events
        List<EventType> events = new ArrayList<>();
        settings.getDescription().getPastEvents().stream()
                .map(event -> adaptEvent(event, true))
                .collect(Collectors.toCollection(() -> events));
        settings.getDescription().getUpcomingEvents().stream()
                .map(event -> adaptEvent(event, false))
                .collect(Collectors.toCollection(() -> events));
        qualifyingParty.setEvent(events);

        // company statement
        EconomicOperatorRoleType economicOperatorRole = new EconomicOperatorRoleType();
        economicOperatorRole.setRoleDescription(Collections.singletonList(settings.getDescription().getCompanyStatement()));
        qualifyingParty.setEconomicOperatorRole(economicOperatorRole);

        qualifyingParty.setParty(company);

        return qualifyingParty;
    }

    public static EventType adaptEvent(CompanyEvent event, Boolean completionIndicator) {
        EventType ublEvent = new EventType();

        // identifier
        ublEvent.setIdentificationID(event.getName());

        // completion indicator
        ublEvent.setCompletionIndicator(completionIndicator);

        // address
        LocationType location = new LocationType();
        location.setAddress(adaptAddress(event.getPlace()));
        ublEvent.setOccurenceLocation(location);

        // start/end data
        PeriodType durationPeriod = new PeriodType();
        durationPeriod.setStartDateItem(event.getDateFrom());
        durationPeriod.setEndDateItem(event.getDateTo());
        ublEvent.setDurationPeriod(durationPeriod);

        // description
        ublEvent.setDescription(event.getDescription());

        return ublEvent;
    }

    public static PartyTaxSchemeType adaptTaxSchema(String vatNumber) {
        CodeType codeType = adaptCodeType(VAT_TAX_TYPE_CODE, vatNumber);
        TaxSchemeType taxScheme = new TaxSchemeType();
        taxScheme.setTaxTypeCode(codeType);
        PartyTaxSchemeType partyTaxSchemeType = new PartyTaxSchemeType();
        partyTaxSchemeType.setTaxScheme(taxScheme);
        return partyTaxSchemeType;
    }

    public static List<CommunicationType> adaptSocialMediaList(List<String> socialMediaList) {
        return socialMediaList.stream().map(sm -> {
            CommunicationType communicationType = new CommunicationType();
            communicationType.setValue(sm);
            return communicationType;
        }).collect(Collectors.toList());
    }

    public static CodeType adaptCodeType(String name, String value) {
        CodeType codeType = new CodeType();
        codeType.setName(name);
        codeType.setValue(value);
        return codeType;
    }

    public static CertificateType adaptCertificate(MultipartFile certFile, String name, String type, String description) throws IOException {

        CodeType codeType = adaptCodeType(name, null);

        BinaryObjectType certificateBinary = new BinaryObjectType();
        certificateBinary.setValue(certFile.getBytes());
        certificateBinary.setFileName(certFile.getOriginalFilename());
        certificateBinary.setMimeCode(certFile.getContentType());
        AttachmentType attachmentType = new AttachmentType();
        attachmentType.setEmbeddedDocumentBinaryObject(certificateBinary);
        DocumentReferenceType documentReferenceType = new DocumentReferenceType();
        documentReferenceType.setAttachment(attachmentType);

        CertificateType certificateType = new CertificateType();
        certificateType.setRemarks(description);
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
                                certificateType.getHjid().toString(),
                                certificateType.getRemarks()))
                .collect(Collectors.toList());
    }

    public static QualityIndicatorType adaptQualityIndicator(QualityIndicatorParameter parameterName, Double value) {
        QualityIndicatorType qualityIndicator = new QualityIndicatorType();
        QuantityType quantity = new QuantityType();
        quantity.setValue(new BigDecimal(value));
        qualityIndicator.setQuantity(quantity);
        qualityIndicator.setQualityParameter(parameterName.name());
        return qualityIndicator;
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
