package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.Address;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.swagger.api.LoginApi;
import eu.nimble.core.infrastructure.identity.swagger.api.RegisterApi;
import eu.nimble.core.infrastructure.identity.swagger.api.RegisterCompanyApi;
import eu.nimble.core.infrastructure.identity.swagger.model.CompanyRegistration;
import eu.nimble.core.infrastructure.identity.swagger.model.Credentials;
import eu.nimble.core.infrastructure.identity.swagger.model.User;
import eu.nimble.core.infrastructure.identity.swagger.model.UserToRegister;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;

@Controller
@SuppressWarnings("PointlessBooleanExpression")
public class UserIdentityController implements LoginApi, RegisterApi, RegisterCompanyApi {

    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private DeliveryTermsRepository deliveryTermsRepository;

    @Autowired
    private PaymentMeansRepository paymentMeansRepository;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Override
    public ResponseEntity<User> registerUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody UserToRegister user) {
        // TODO: remove this method?
        return null;
    }

    @Override
    public ResponseEntity<CompanyRegistration> registerCompany(
            @ApiParam(value = "Company object that needs to be registered to Nimble.", required = true) @RequestBody CompanyRegistration company) {

        // check if user already exists
        if (uaaUserRepository.findByUsername(company.getEmail()).isEmpty() == false)
            return new ResponseEntity<>(HttpStatus.CONFLICT);

        logger.info("Registering company with user " + company.getEmail());

        // create UBL person
        PersonType adminPerson = new PersonType();
        adminPerson.setFirstName(company.getFirstname());
        adminPerson.setFamilyName(company.getLastname());
        adminPerson.setJobTitle(company.getJobTitle());
//        adminPerson.setBirthDate(company.getDateOfBirth()); // TODO: convert date
        adminPerson.setBirthplaceName(company.getPlaceOfBirth());
        ContactType contact = new ContactType();
        contact.setElectronicMail(company.getEmail());
        contact.setTelephone(company.getPhoneNumber());
        adminPerson.setContact(contact);
        personRepository.save(adminPerson);

        // update id of person
        adminPerson.setID(UblUtils.identifierType(adminPerson.getHjid()));
        personRepository.save(adminPerson);

        // create UAA user
        UaaUser uaaUser = new UaaUser(company.getEmail(), company.getPassword(), adminPerson);
        uaaUserRepository.save(uaaUser);

        // create company
        PartyType companyParty = new PartyType();
        PartyNameType companyName = new PartyNameType();
        companyName.setName(company.getCompanyName());
        companyParty.getPartyName().add(companyName);
        companyParty.getPerson().add(adminPerson);
        partyRepository.save(companyParty);

        // create address
        companyParty.setPostalAddress(UblUtils.addressType(company.getCompanyCountry(), company.getCompanyAddress()));

        // create delivery terms
        DeliveryTermsType blankDeliveryTerms = new DeliveryTermsType();
        deliveryTermsRepository.save(blankDeliveryTerms);
        blankDeliveryTerms.setID(UblUtils.identifierType(blankDeliveryTerms.getHjid()));
        deliveryTermsRepository.save(blankDeliveryTerms);
        companyParty.getDeliveryTerms().add(blankDeliveryTerms);

        // create payment means
        PaymentMeansType paymentMeans = UblUtils.emptyUBLObject(new PaymentMeansType());
        paymentMeansRepository.save(paymentMeans);
        paymentMeans.setID(UblUtils.identifierType(paymentMeans.getHjid()));
        paymentMeansRepository.save(paymentMeans);
        companyParty.getPaymentMeans().add(paymentMeans);

        // update id of company
        companyParty.setID(UblUtils.identifierType(companyParty.getHjid()));
        partyRepository.save(companyParty);

        // add id to original object
        company.setCompanyID(companyParty.getID().getValue());
        company.setUserID(adminPerson.getID().getValue());
        return new ResponseEntity<>(company, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<CompanyRegistration> loginUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody Credentials credentials) {

        logger.info("User " + credentials.getEmail() + " wants to login...");

        List<UaaUser> potentialUsers = uaaUserRepository.findByUsername(credentials.getEmail());
        if(potentialUsers.isEmpty()) {
            logger.info("User " + credentials.getEmail() + " not found.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UaaUser potentialUser = potentialUsers.get(0);
        if (potentialUser.getPassword().equals(credentials.getPassword()) == false) {
            logger.info("User " + credentials.getEmail() + " entered wrong password.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        CompanyRegistration retVal = new CompanyRegistration();
        retVal.setUsername(potentialUser.getUsername());
        retVal.setFirstname(potentialUser.getUBLPerson().getFirstName());
        retVal.setLastname(potentialUser.getUBLPerson().getFamilyName());
        retVal.setUserID(potentialUser.getUBLPerson().getHjid().toString());

        // find company of user
        List<PartyType> companies = partyRepository.findByPerson(potentialUser.getUBLPerson());
        if (companies.isEmpty() == false) {
            PartyType company = companies.get(0);
            retVal.setCompanyID(company.getHjid().toString());
        }

        logger.info("User " + credentials.getEmail() + " sucessfully logged in.");

        return new ResponseEntity<>(retVal, HttpStatus.OK);
    }
}