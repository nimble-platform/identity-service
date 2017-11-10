package eu.nimble.core.infrastructure.identity.utils;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;

import java.util.List;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@SuppressWarnings("WeakerAccess")
public class UblAdapter {

    public static CompanySettings adaptCompanySettings(PartyType partyType) {
        CompanySettings settings = new CompanySettings();
        settings.setName(partyType.getName());
        settings.setAddress(adaptAddress(partyType.getPostalAddress()));
        partyType.getPaymentMeans().stream().findFirst().ifPresent(means -> settings.setPaymentMeans(adaptPaymentMeans(means)));
        partyType.getDeliveryTerms().stream().findFirst()
                .ifPresent(deliveryTermsType -> settings.setDeliveryTerms(adaptDeliveryTerms(deliveryTermsType)));

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

        return dtoDeliveryTerms;
    }

    public static DeliveryTermsType adaptDeliveryTerms(DeliveryTerms dtoDeliveryTerms) {

        if (dtoDeliveryTerms == null)
            return new DeliveryTermsType();

        DeliveryTermsType ublDeliveryTerms = new DeliveryTermsType();
        ublDeliveryTerms.setSpecialTerms(dtoDeliveryTerms.getSpecialTerms());

        // ToDo: create GregorianCalender and add 'EstimatedDeliveryTime'.

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
        companyParty.setName(registration.getName());
        companyParty.getPerson().add(admin);
        companyParty.setPostalAddress(adaptAddress(registration.getAddress()));

        return companyParty;
    }
}
