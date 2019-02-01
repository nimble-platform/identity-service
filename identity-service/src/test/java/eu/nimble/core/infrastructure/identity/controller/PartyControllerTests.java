package eu.nimble.core.infrastructure.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.nimble.core.infrastructure.identity.IdentityServiceApplication;
import eu.nimble.core.infrastructure.identity.config.DefaultTestConfiguration;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.dto.CompanyRegistration;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.repository.QualifyingPartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.hamcrest.Matchers;
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

import static eu.nimble.core.infrastructure.identity.TestUtils.JSON_DATE_FORMAT;
import static eu.nimble.core.infrastructure.identity.TestUtils.createCompanyRegistration;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Created by Johannes Innerbichler on 2019-02-01.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = IdentityServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Import(DefaultTestConfiguration.class)
public class PartyControllerTests {

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
    private ObjectMapper objectMapper;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, "topic");

    @Test
    public void testPartyAll() throws Exception {
        // register companies
        CompanyRegistration registration1 = registerCompany("company1");
        CompanyRegistration registration2 = registerCompany("company2");

        // fetch list
        this.mockMvc.perform(get("/party/all").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].name", Matchers.isOneOf("company1", "company2")))
                .andExpect(jsonPath("$[0].identifier", Matchers.isOneOf("1", "2")))
                .andExpect(jsonPath("$[1].name", Matchers.isOneOf("company1", "company2")))
                .andExpect(jsonPath("$[1].identifier", Matchers.isOneOf(registration1.getCompanyID().toString(), registration2.getCompanyID().toString())));
    }

    private CompanyRegistration registerCompany(String legalName) throws Exception {
        PersonType person = personRepository.save(new PersonType());
        uaaUserRepository.save(new UaaUser("testuser", person, "externalID"));
        CompanyRegistration companyRegistration = createCompanyRegistration(legalName, person);

        // GIVEN: existing company on platform
        Gson gson = new GsonBuilder().setDateFormat(JSON_DATE_FORMAT).create();
        String responseAsString = this.mockMvc.perform(post("/register/company").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .content(gson.toJson(companyRegistration)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        companyRegistration = objectMapper.readValue(responseAsString, CompanyRegistration.class);

        return companyRegistration;
    }
}
