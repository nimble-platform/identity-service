package eu.nimble.core.infrastructure.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.nimble.core.infrastructure.identity.IdentityServiceApplication;
import eu.nimble.core.infrastructure.identity.DefaultTestConfiguration;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by Johannes Innerbichler on 09.08.18.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = IdentityServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder
@Import(DefaultTestConfiguration.class)
public class AdminControllerTests {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private QualifyingPartyRepository qualifyingPartyRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private DeliveryTermsRepository deliveryTermsRepository;

    @Autowired PaymentMeansRepository paymentMeansRepository;

    @Autowired
    private DocumentReferenceRepository documentReferenceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, "topic");

    @Test
    public void testDeleteCompany() throws Exception {
        PersonType person = personRepository.save(new PersonType());
        uaaUserRepository.save(new UaaUser("testuser", person, "externalID"));
        CompanyRegistration companyRegistration = createCompanyRegistration(person);

        // GIVEN: existing company on platform
        Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
        String responseAsString = this.mockMvc.perform(post("/register/company").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .content(gson.toJson(companyRegistration)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        companyRegistration = objectMapper.readValue(responseAsString, CompanyRegistration.class);

        // check list of all parties whether it contains newly registered company
        this.mockMvc.perform(get("/parties/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].name", is(companyRegistration.getSettings().getDetails().getCompanyLegalName())))
                .andExpect(jsonPath("$.content[0].id", is(companyRegistration.getCompanyID().toString())));

        // check repositories
        assertEquals(this.partyRepository.count(),1);
        assertEquals(this.qualifyingPartyRepository.count(),1);
        assertEquals(this.personRepository.count(),1);
        assertEquals(this.uaaUserRepository.count(),1);
        assertEquals(this.deliveryTermsRepository.count(),1);
        assertEquals(this.paymentMeansRepository.count(),1);
        assertEquals(this.documentReferenceRepository.count(),1);

        // WHEN: deleting company
        this.mockMvc.perform(delete("/admin/delete_company/" + companyRegistration.getCompanyID()))
                .andExpect(status().isOk());

        // THEN: company should be deleted
        this.mockMvc.perform(get("/parties/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(0)));

        // check repositories
        assertEquals(this.partyRepository.count(),0);
        assertEquals(this.qualifyingPartyRepository.count(),0);
        assertEquals(this.personRepository.count(),0);
        assertEquals(this.uaaUserRepository.count(),0);
        assertEquals(this.deliveryTermsRepository.count(),0);
        assertEquals(this.paymentMeansRepository.count(),0);
        assertEquals(this.documentReferenceRepository.count(),0);
    }

    private static CompanyRegistration createCompanyRegistration(PersonType user) {

        CompanyDetails companyDetails = new CompanyDetails("legal name", "vat number", "verification info",
                new Address(), "business type", Collections.singletonList("business type"), 1970,
                Collections.singletonList("industry sector"));
        CompanyDescription companyDescription = new CompanyDescription("statement", "website",
                Collections.singletonList("photos"), "imageId", Collections.singletonList("social media"),
                Collections.singletonList(new CompanyEvent()), Collections.singletonList("test"));
        CompanyTradeDetails companyTradeDetails = new CompanyTradeDetails();
        CompanySettings companySettings = new CompanySettings("123", companyDetails, companyDescription,
                companyTradeDetails, Collections.singletonList(new CompanyCertificate()), Collections.singleton("category 1"), Collections.singleton("category 2"));
        CompanyRegistration companyRegistration = new CompanyRegistration();
        companyRegistration.setCompanyID(123L);
        companyRegistration.setUserID(user.getHjid());
        companyRegistration.setAccessToken("DUMMY ACCESSTOKEN");
        companyRegistration.setSettings(companySettings);

        return companyRegistration;
    }
}
