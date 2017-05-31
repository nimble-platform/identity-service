package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entities.UaaUser;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.swagger.api.LoginApi;
import eu.nimble.core.infrastructure.identity.swagger.api.RegisterApi;
import eu.nimble.core.infrastructure.identity.swagger.api.RegisterCompanyApi;
import eu.nimble.core.infrastructure.identity.swagger.model.CompanyRegistration;
import eu.nimble.core.infrastructure.identity.swagger.model.Credentials;
import eu.nimble.core.infrastructure.identity.swagger.model.User;
import eu.nimble.core.infrastructure.identity.swagger.model.UserToRegister;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ContactType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("PointlessBooleanExpression")
@Controller
public class UserIdentityController implements LoginApi, RegisterApi, RegisterCompanyApi {

    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PartyRepository partyRepository;

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

        // create UAA user
        UaaUser uaaUser = new UaaUser(company.getEmail(), company.getPassword(), adminPerson);
        uaaUserRepository.save(uaaUser);

        // create ubl company
        PartyType companyParty = new PartyType();
        PartyNameType companyName = new PartyNameType();
        companyName.setName(company.getCompanyName());
        companyParty.setPartyName(Collections.singletonList(companyName));
        companyParty.setPerson(Collections.singletonList(adminPerson));
        partyRepository.save(companyParty);

        company.setCompanyID(companyParty.getHjid().toString());
        company.setUserID(adminPerson.getHjid().toString());
        return new ResponseEntity<>(company, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<CompanyRegistration> loginUser(
            @ApiParam(value = "User object that needs to be registered to Nimble.", required = true) @RequestBody Credentials credentials) {

        logger.info("User " + credentials.getEmail() + " wants to login...");

        List<UaaUser> potentialUsers = uaaUserRepository.findByUsername(credentials.getEmail());
        if(potentialUsers.isEmpty())
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        UaaUser potentialUser = potentialUsers.get(0);
        if (potentialUser.getPassword().equals(credentials.getPassword()) == false)
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        CompanyRegistration retVal = new CompanyRegistration();
        retVal.setUserID(potentialUser.getUBLPerson().getHjid().toString());

        logger.info("User " + credentials.getEmail() + " sucessfully logged in.");

        return new ResponseEntity<>(retVal, HttpStatus.OK);
    }
}