package eu.nimble.core.infrastructure.identity.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.nimble.core.infrastructure.identity.IdentityServiceApplication;
import eu.nimble.core.infrastructure.identity.config.DefaultTestConfiguration;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties.LanguageID.ENGLISH;
import static eu.nimble.core.infrastructure.identity.TestUtils.JSON_DATE_FORMAT;
import static eu.nimble.core.infrastructure.identity.TestUtils.createCompanyRegistration;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class AdminControllerTests {

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
        CompanyRegistration companyRegistration = createCompanyRegistration("company_name", person);

        // GIVEN: existing company on platform
        Gson gson = new GsonBuilder().setDateFormat(JSON_DATE_FORMAT).create();
        String responseAsString = this.mockMvc.perform(post("/register/company").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .content(gson.toJson(companyRegistration)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        companyRegistration = objectMapper.readValue(responseAsString, CompanyRegistration.class);

        // check list of all parties whether it contains newly registered company
        this.mockMvc.perform(get("/parties/all?page=0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].partyName[0].name.value", is(companyRegistration.getSettings().getDetails().getLegalName().get(ENGLISH))))
                .andExpect(jsonPath("$.content[0].partyIdentification[0].id", is(companyRegistration.getCompanyID().toString())));

        // check repositories
        assertEquals(1, this.partyRepository.count());
        assertEquals(1, this.qualifyingPartyRepository.count());
        assertEquals(1, this.personRepository.count());
        assertEquals(1, this.uaaUserRepository.count());
        assertEquals(1, this.deliveryTermsRepository.count());
        assertEquals(1, this.paymentMeansRepository.count());
        assertEquals(1, this.documentReferenceRepository.count());

        // WHEN: deleting company
        this.mockMvc.perform(delete("/admin/delete_company/" + companyRegistration.getCompanyID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN"))
                .andExpect(status().isOk());

        // THEN: company should be deleted
        this.mockMvc.perform(get("/parties/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(0)));

        // check repositories
        assertEquals(0, this.partyRepository.count());
        assertEquals(0, this.qualifyingPartyRepository.count());
        assertEquals(0, this.personRepository.count());
        assertEquals(0, this.uaaUserRepository.count());
        assertEquals(0, this.deliveryTermsRepository.count());
        assertEquals(0, this.paymentMeansRepository.count());
        assertEquals(0, this.documentReferenceRepository.count());
    }
}
