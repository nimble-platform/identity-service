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
    public static final String DOCUMENT_TYPE_COMPANY_PHOTO = "CompanyPhoto";
    public static final String DOCUMENT_TYPE_COMPANY_LOGO = "CompanyLogo";
    public static final String DOCUMENT_TYPE_EXTERNAL_RESOURCE = "ExternalResource";

    public static CompanySettings adaptCompanySettings(PartyType party, QualifyingPartyType qualifyingParty) {
        CompanySettings settings = new CompanySettings();

        CompanyDetails companyDetails = adaptCompanyDetails(party, qualifyingParty);
        CompanyDescription companyDescription = adaptCompanyDescription(party, qualifyingParty);

        settings.setCompanyID(party.getID());
        settings.setDetails(companyDetails);
        settings.setDescription(companyDescription);

        // set payment means
        if (party.getPurchaseTerms() != null) {
            List<PaymentMeans> paymentMeans = party.getPurchaseTerms().getPaymentMeans().stream()
                    .map(UblAdapter::adaptPaymentMeans)
                    .collect(Collectors.toList());
            settings.getTradeDetails().setPaymentMeans(paymentMeans);
        }

        // set delivery terms
        if (party.getPurchaseTerms() != null) {
            List<DeliveryTerms> deliveryTerms = party.getPurchaseTerms().getDeliveryTerms().stream()
                    .map(UblAdapter::adaptDeliveryTerms)
                    .collect(Collectors.toList());
            settings.getTradeDetails().getDeliveryTerms().clear();
            settings.getTradeDetails().getDeliveryTerms().addAll(deliveryTerms);
        }

        if (party.getPpapCompatibilityLevel() != null)
            settings.getTradeDetails().setPpapCompatibilityLevel(party.getPpapCompatibilityLevel().intValue());

        // set certificates
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
        settings.getRecentlyUsedProductCategories().clear();
        settings.getRecentlyUsedProductCategories().addAll(recentlyUsedProductCategories);

        // set industry sectors
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
                .filter(scheme -> scheme != null && scheme.getTaxScheme() != null && scheme.getTaxScheme().getTaxTypeCode() != null)
                .filter(scheme -> VAT_TAX_TYPE_CODE.equals(scheme.getTaxScheme().getTaxTypeCode().getName()))
                .filter(scheme -> scheme.getTaxScheme().getTaxTypeCode().getValue() != null)
                .map(scheme -> scheme.getTaxScheme().getTaxTypeCode().getValue())
                .findFirst().orElse(null));
        companyDetails.setAddress(adaptAddress(party.getPostalAddress()));
        if (party.getIndustryClassificationCode() != null)
            companyDetails.setBusinessType(party.getIndustryClassificationCode().getValue());

        if (qualifyingParty != null) {
            companyDetails.setVerificationInformation(qualifyingParty.getBusinessIdentityEvidenceID());
            companyDetails.setBusinessKeywords(qualifyingParty.getBusinessClassificationScheme().getDescription());
            if (qualifyingParty.getOperatingYearsQuantity() != null && qualifyingParty.getOperatingYearsQuantity().getValue() != null)
                companyDetails.setYearOfCompanyRegistration(qualifyingParty.getOperatingYearsQuantity().getValue().intValue());
        }

        return companyDetails;
    }

    public static CompanyDescription adaptCompanyDescription(PartyType party, QualifyingPartyType qualifyingPartyType) {

        CompanyDescription companyDescription = new CompanyDescription();
        companyDescription.setWebsite(party.getWebsiteURI());
        if (party.getContact() != null && party.getContact().getOtherCommunication() != null) {
            companyDescription.setSocialMediaList(party.getContact().getOtherCommunication()
                    .stream()
                    .map(CommunicationType::getValue)
                    .collect(Collectors.toList()));
        }

        // photos
        if (party.getDocumentReference() != null) {
            List<String> companyPhotoIds = party.getDocumentReference().stream()
                    .filter(dr -> DOCUMENT_TYPE_COMPANY_PHOTO.equals(dr.getDocumentType()))
                    .map(dr -> dr.getHjid().toString())
                    .collect(Collectors.toList());
            companyDescription.setCompanyPhotoList(companyPhotoIds);

            // company logo
            String logoImageId = party.getDocumentReference().stream()
                    .filter(dr -> DOCUMENT_TYPE_COMPANY_LOGO.equals(dr.getDocumentType()))
                    .map(dr -> dr.getHjid().toString())
                    .findFirst()
                    .orElse(null);
            companyDescription.setLogoImageId(logoImageId);

            // external resources
            List<String> externalResources = UblAdapter.adaptExternalResourcesType(party.getDocumentReference());
            companyDescription.setExternalResources(externalResources);
        }

        if (qualifyingPartyType != null) {
            if (qualifyingPartyType.getEconomicOperatorRole() != null && qualifyingPartyType.getEconomicOperatorRole().getRoleDescription().isEmpty() == false)
                companyDescription.setCompanyStatement(qualifyingPartyType.getEconomicOperatorRole().getRoleDescription().get(0));

            // adapt events
            if (qualifyingPartyType.getEvent() != null) {
                List<CompanyEvent> events = qualifyingPartyType.getEvent().stream()
                        .map(UblAdapter::adaptEvent)
                        .collect(Collectors.toList());
                companyDescription.getEvents().clear();
                companyDescription.getEvents().addAll(events);
            }
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
        return adaptCompanySettings(registration.getSettings(), representative, null);
    }

    public static PartyType adaptCompanySettings(CompanySettings settings, PersonType representative, PartyType companyToChange) {

        if (companyToChange == null)
            companyToChange = new PartyType();

        // legal name
        companyToChange.setName(settings.getDetails().getCompanyLegalName());

        // VAT number
        if (settings.getDetails().getVatNumber() != null)
            companyToChange.getPartyTaxScheme().add(adaptTaxSchema(settings.getDetails().getVatNumber()));

        // postal address
        companyToChange.setPostalAddress(adaptAddress(settings.getDetails().getAddress()));

        // classification code
        companyToChange.setIndustryClassificationCode(adaptCodeType("IndustryClassificationCode", settings.getDetails().getBusinessType()));

        // website URL
        if (settings.getDescription() != null) {
            companyToChange.setWebsiteURI(settings.getDescription().getWebsite());

            // social media list
            ContactType socialMediaContact = new ContactType();
            socialMediaContact.setOtherCommunication(adaptSocialMediaList(settings.getDescription().getSocialMediaList()));
            companyToChange.setContact(socialMediaContact);

            // external resources
            List<DocumentReferenceType> existingExternalResources = companyToChange.getDocumentReference().stream()
                    .filter(d -> DOCUMENT_TYPE_EXTERNAL_RESOURCE.equals(d.getDocumentType()))
                    .collect(Collectors.toList());
            List<DocumentReferenceType> newExternalResources = UblAdapter.adaptExternalResources(settings.getDescription().getExternalResources());
            companyToChange.getDocumentReference().removeAll(existingExternalResources);
            companyToChange.getDocumentReference().addAll(newExternalResources);
        }

        // industry sectors
        List<CodeType> industrySectors = UblAdapter.adaptIndustrySectors(settings.getDetails().getIndustrySectors());
        companyToChange.getIndustrySector().clear();
        companyToChange.getIndustrySector().addAll(industrySectors);

        // check if representative is not already in list
        if (representative != null) {
            boolean alreadyInList = companyToChange.getPerson()
                    .stream()
                    .anyMatch(personType -> personType.getContact().getElectronicMail().equals(representative.getContact().getElectronicMail()));
            if (alreadyInList == false)
                companyToChange.getPerson().add(representative);
        }


        if (settings.getTradeDetails() != null) {

            if (companyToChange.getPurchaseTerms() == null)
                companyToChange.setPurchaseTerms(new TradingPreferences());

            // delivery terms
            List<DeliveryTermsType> deliveryTerms = settings.getTradeDetails().getDeliveryTerms().stream()
                    .map(UblAdapter::adaptDeliveryTerms)
                    .collect(Collectors.toList());
            companyToChange.getPurchaseTerms().getDeliveryTerms().clear();
            companyToChange.getPurchaseTerms().getDeliveryTerms().addAll(deliveryTerms);

            // payment means
            List<PaymentMeansType> paymentMeans = settings.getTradeDetails().getPaymentMeans().stream()
                    .map(UblAdapter::adaptPaymentMeans)
                    .collect(Collectors.toList());
            companyToChange.getPurchaseTerms().getPaymentMeans().clear();
            companyToChange.getPurchaseTerms().getPaymentMeans().addAll(paymentMeans);

            // PPAP
            int ppapLevel = settings.getTradeDetails().getPpapCompatibilityLevel() != null ? settings.getTradeDetails().getPpapCompatibilityLevel() : 0;
            companyToChange.setPpapCompatibilityLevel(BigDecimal.valueOf(ppapLevel));
        }

        return companyToChange;
    }

    public static QualifyingPartyType adaptQualifyingParty(CompanySettings settings, PartyType company) {
        return adaptQualifyingParty(settings, company, null);
    }

    public static QualifyingPartyType adaptQualifyingParty(CompanySettings settings, PartyType company, QualifyingPartyType existingQualifyingParty) {

        QualifyingPartyType qualifyingParty = existingQualifyingParty == null ? new QualifyingPartyType() : existingQualifyingParty;

        // set verification info
        qualifyingParty.setBusinessIdentityEvidenceID(settings.getDetails().getVerificationInformation());

        // business keywords
        ClassificationSchemeType classificationScheme = new ClassificationSchemeType();
        classificationScheme.setDescription(new ArrayList<>(settings.getDetails().getBusinessKeywords()));
        qualifyingParty.setBusinessClassificationScheme(classificationScheme);

        // year of company registration
        if (settings.getDetails() != null && settings.getDetails().getYearOfCompanyRegistration() != null) {
            QuantityType years = new QuantityType();
            years.setValue(new BigDecimal(settings.getDetails().getYearOfCompanyRegistration()));
            qualifyingParty.setOperatingYearsQuantity(years);
        }

        // company events
        if (settings.getDescription() != null) {
            List<EventType> events = new ArrayList<>();
            settings.getDescription().getEvents().stream()
                    .map(UblAdapter::adaptEvent)
                    .collect(Collectors.toCollection(() -> events));
            qualifyingParty.getEvent().clear();
            qualifyingParty.getEvent().addAll(events);

            // company statement
            EconomicOperatorRoleType economicOperatorRole = new EconomicOperatorRoleType();
            economicOperatorRole.setRoleDescription(Collections.singletonList(settings.getDescription().getCompanyStatement()));
            qualifyingParty.setEconomicOperatorRole(economicOperatorRole);
        }

        qualifyingParty.setParty(company);

        return qualifyingParty;
    }

    public static List<DocumentReferenceType> adaptExternalResources(List<String> externalResources) {
        return externalResources.stream()
                .map(er -> {
                    ExternalReferenceType externalReference = new ExternalReferenceType();
                    externalReference.setURI(er);
                    AttachmentType attachment = new AttachmentType();
                    attachment.setExternalReference(externalReference);

                    DocumentReferenceType documentReference = new DocumentReferenceType();
                    documentReference.setDocumentType(DOCUMENT_TYPE_EXTERNAL_RESOURCE);
                    documentReference.setAttachment(attachment);
                    return documentReference;
                })
                .collect(Collectors.toList());
    }


    public static List<String> adaptExternalResourcesType(List<DocumentReferenceType> externalResources) {
        return externalResources.stream()
                .filter(er -> DOCUMENT_TYPE_EXTERNAL_RESOURCE.equals(er.getDocumentType()))
                .filter(er -> er.getAttachment() != null && er.getAttachment().getExternalReference() != null)
                .map(er -> er.getAttachment().getExternalReference().getURI())
                .collect(Collectors.toList());
    }

    public static EventType adaptEvent(CompanyEvent event) {
        EventType ublEvent = new EventType();

        // identifier
        ublEvent.setIdentificationID(event.getName());

//        // completion indicator
//        Boolean completionIndicator = new Date().after(event.getDateTo());
//        ublEvent.setCompletionIndicator(completionIndicator);

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

    public static CompanyEvent adaptEvent(EventType ublEvent) {
        CompanyEvent companyEvent = new CompanyEvent();
        companyEvent.setDescription(ublEvent.getDescription());
        companyEvent.setName(ublEvent.getIdentificationID());
        if (ublEvent.getOccurenceLocation() != null)
            companyEvent.setPlace(adaptAddress(ublEvent.getOccurenceLocation().getAddress()));
        if (ublEvent.getDurationPeriod() != null) {
            companyEvent.setDateFrom(ublEvent.getDurationPeriod().getStartDateItem());
            companyEvent.setDateTo(ublEvent.getDurationPeriod().getEndDateItem());
        }
        return companyEvent;
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

        if (socialMediaList == null)
            return Collections.emptyList();

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

    public static DocumentReferenceType adaptCompanyPhoto(BinaryObjectType photoBinary, Boolean isLogo) {

        AttachmentType attachment = new AttachmentType();
        attachment.setEmbeddedDocumentBinaryObject(photoBinary);

        DocumentReferenceType document = new DocumentReferenceType();
        String documentType = isLogo ? DOCUMENT_TYPE_COMPANY_LOGO : DOCUMENT_TYPE_COMPANY_PHOTO;
        document.setDocumentType(documentType);
        document.setAttachment(attachment);
        return document;
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
