package eu.nimble.core.infrastructure.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.nimble.core.infrastructure.identity.IdentityServiceApplication;
import eu.nimble.core.infrastructure.identity.config.DefaultTestConfiguration;
import eu.nimble.core.infrastructure.identity.repository.NegotiationSettingsRepository;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualityIndicatorType;
import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static eu.nimble.service.model.ubl.extension.QualityIndicatorParameter.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by Johannes Innerbichler on 09.08.18.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = IdentityServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Import(DefaultTestConfiguration.class)
public class CompanySettingsControllerTests {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private NegotiationSettingsRepository negotiationSettingsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, "topic");

    @Test
    public void testCreateCompanySettings() throws Exception {

        // GIVEN: existing company on platform
        PartyType company = identityService.getCompanyOfUser(null).get();
        partyRepository.save(company);
        company.setID(company.getHjid().toString());
        partyRepository.save(company);

        // WHEN: updating company settings
        CompanyDetails companyDetails = new CompanyDetails();
        companyDetails.setCompanyLegalName("company name");
        companyDetails.setVatNumber("vat number");
        companyDetails.setVerificationInformation("verification number");
        companyDetails.setAddress(new Address("street name", "building number", "city name", "postal code", "country"));
        companyDetails.setBusinessKeywords(Arrays.asList("k1", "k2"));
        companyDetails.setBusinessType("business type");
        companyDetails.setYearOfCompanyRegistration(2001);
        companyDetails.getIndustrySectors().add("industry sector 1");
        companyDetails.getIndustrySectors().add("industry sector 2");

        CompanyDescription companyDescription = new CompanyDescription();
        companyDescription.setCompanyStatement("company statement");
        companyDescription.setWebsite("website");
        companyDescription.setSocialMediaList(Arrays.asList("social media 1", "social media 2"));
        Address eventAddress = new Address("event street", "event building", "event city", "event postal", "event country");
        Date eventDate = new Date();
        companyDescription.getEvents().add(new CompanyEvent("event name", eventAddress, eventDate, eventDate, "event description"));
        companyDescription.setExternalResources(Arrays.asList("URL 1", "URL 2"));

        CompanyTradeDetails companyTradeDetails = new CompanyTradeDetails();
        companyTradeDetails.setPpapCompatibilityLevel(5);
        companyTradeDetails.getPaymentMeans().add(new PaymentMeans("instruction note"));
        companyTradeDetails.getDeliveryTerms().add(new DeliveryTerms("special terms", new Address(), 5));

        CompanySettings companySettings = new CompanySettings();
        companySettings.setDetails(companyDetails);
        companySettings.setDescription(companyDescription);
        companySettings.setTradeDetails(companyTradeDetails);
        companySettings.getPreferredProductCategories().add("category 1");
        companySettings.getPreferredProductCategories().add("category 2");
        companySettings.getRecentlyUsedProductCategories().add("category 3");
        companySettings.getRecentlyUsedProductCategories().add("category 4");

        Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
        this.mockMvc.perform(put("/company-settings/" + company.getID())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .content(gson.toJson(companySettings)))
                .andExpect(status().isAccepted());

        // THEN: getting settings should be updated
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        this.mockMvc.perform(get("/company-settings/" + company.getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // check details
                .andExpect(jsonPath("$.details.companyLegalName", is("company name")))
                .andExpect(jsonPath("$.details.vatNumber", is("vat number")))
                .andExpect(jsonPath("$.details.verificationInformation", is("verification number")))
                .andExpect(jsonPath("$.details.address.streetName", is("street name")))
                .andExpect(jsonPath("$.details.address.buildingNumber", is("building number")))
                .andExpect(jsonPath("$.details.address.cityName", is("city name")))
                .andExpect(jsonPath("$.details.address.postalCode", is("postal code")))
                .andExpect(jsonPath("$.details.address.country", is("country")))
                .andExpect(jsonPath("$.details.businessKeywords.length()", is(2)))
                .andExpect(jsonPath("$.details.businessType", is("business type")))
                .andExpect(jsonPath("$.details.businessKeywords[0]", is("k1")))
                .andExpect(jsonPath("$.details.businessKeywords[1]", is("k2")))
                .andExpect(jsonPath("$.details.yearOfCompanyRegistration", is(2001)))
                .andExpect(jsonPath("$.details.industrySectors.length()", is(2)))
                .andExpect(jsonPath("$.details.industrySectors[0]", is("industry sector 1")))
                .andExpect(jsonPath("$.details.industrySectors[1]", is("industry sector 2")))
                // check description
                .andExpect(jsonPath("$.description.companyStatement", is("company statement")))
                .andExpect(jsonPath("$.description.website", is("website")))
                .andExpect(jsonPath("$.description.socialMediaList.length()", is(2)))
                .andExpect(jsonPath("$.description.socialMediaList[0]", is("social media 1")))
                .andExpect(jsonPath("$.description.socialMediaList[1]", is("social media 2")))
                .andExpect(jsonPath("$.description.events.length()", is(1)))
                .andExpect(jsonPath("$.description.events[0].dateTo", is(format.format(eventDate))))
                .andExpect(jsonPath("$.description.externalResources.length()", is(2)))
                .andExpect(jsonPath("$.description.externalResources[0]", is("URL 1")))
                .andExpect(jsonPath("$.description.externalResources[1]", is("URL 2")))
                // check certificates
                .andExpect(jsonPath("$.certificates.length()", is(0))) // no certs added
                // check trade details
                .andExpect(jsonPath("$.tradeDetails.ppapCompatibilityLevel", is(5)))
                .andExpect(jsonPath("$.tradeDetails.paymentMeans.length()", is(1)))
                .andExpect(jsonPath("$.tradeDetails.paymentMeans.[0].instructionNote", is("instruction note")))
                .andExpect(jsonPath("$.tradeDetails.deliveryTerms.length()", is(1)))
                .andExpect(jsonPath("$.tradeDetails.deliveryTerms[0].specialTerms", is("special terms")))
                .andExpect(jsonPath("$.tradeDetails.deliveryTerms[0].estimatedDeliveryTime", is(5)))
                // product categories
                .andExpect(jsonPath("$.preferredProductCategories.length()", is(2)))
                .andExpect(jsonPath("$.preferredProductCategories.length()", is(2)))
                .andExpect(jsonPath("$.preferredProductCategories", hasItem("category 1")))
                .andExpect(jsonPath("$.preferredProductCategories", hasItem("category 2")))
                .andExpect(jsonPath("$.recentlyUsedProductCategories.length()", is(2)))
                .andExpect(jsonPath("$.recentlyUsedProductCategories", hasItem("category 3")))
                .andExpect(jsonPath("$.recentlyUsedProductCategories", hasItem("category 4")));
    }

    @Test
    public void testSimpleAddNegotiationSettings() throws Exception {

        // GIVEN + WHEN: existing company on platform
        NegotiationSettings negotiationSettings = this.initNegotiationSettings();

        // THEN: settings should be updated
        this.mockMvc.perform(get("/company-settings/negotiation/" + negotiationSettings.getCompany().getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // check warranty period settings
                .andExpect(jsonPath("$.warrantyPeriodUnits.length()", is(1)))
                .andExpect(jsonPath("$.warrantyPeriodUnits[0]", is("weeks")))
                .andExpect(jsonPath("$.warrantyPeriodRanges.length()", is(1)))
                .andExpect(jsonPath("$.warrantyPeriodRanges[0].start", is(5)))
                .andExpect(jsonPath("$.warrantyPeriodRanges[0].end", is(6)))
                // check delivery period settings
                .andExpect(jsonPath("$.deliveryPeriodUnits.length()", is(2)))
                .andExpect(jsonPath("$.deliveryPeriodUnits[0]", is("days")))
                .andExpect(jsonPath("$.deliveryPeriodUnits[1]", is("weeks")))
                .andExpect(jsonPath("$.deliveryPeriodRanges[0].start", is(1)))
                .andExpect(jsonPath("$.deliveryPeriodRanges.length()", is(2)))
                .andExpect(jsonPath("$.deliveryPeriodRanges[0].end", is(3)))
                .andExpect(jsonPath("$.deliveryPeriodRanges[1].start", is(2)))
                .andExpect(jsonPath("$.deliveryPeriodRanges[1].end", is(4)))
                // check incoterms
                .andExpect(jsonPath("$.incoterms.length()", is(2)))
                .andExpect(jsonPath("$.incoterms[0]", is("inco_1")))
                .andExpect(jsonPath("$.incoterms[1]", is("inco_2")))
                // check payment settings
                .andExpect(jsonPath("$.paymentMeans.length()", is(1)))
                .andExpect(jsonPath("$.paymentMeans[0]", is("pm_1")))
                .andExpect(jsonPath("$.paymentTerms.length()", is(1)))
                .andExpect(jsonPath("$.paymentTerms[0]", is("pt_1")));
    }

    @Test
    public void testSimpleRemoveNegotiationSettings() throws Exception {

        // GIVEN: existing company on platform
        NegotiationSettings negotiationSettings = this.initNegotiationSettings();

        // WHEN: setting empty negotiation settings
        NegotiationSettings emptySettings = new NegotiationSettings();

        Gson gson = new Gson();
        this.mockMvc.perform(put("/company-settings/negotiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(emptySettings))
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // THEN: settings should be empty
        this.mockMvc.perform(get("/company-settings/negotiation/" + negotiationSettings.getCompany().getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.warrantyPeriodUnits.length()", is(0)))
                .andExpect(jsonPath("$.warrantyPeriodRanges.length()", is(0)))
                .andExpect(jsonPath("$.deliveryPeriodUnits.length()", is(0)))
                .andExpect(jsonPath("$.deliveryPeriodRanges.length()", is(0)))
                .andExpect(jsonPath("$.incoterms.length()", is(0)))
                .andExpect(jsonPath("$.paymentMeans.length()", is(0)))
                .andExpect(jsonPath("$.paymentTerms.length()", is(0)));
    }

    @Test
    public void testSimpleUpdateNegotiationSettings() throws Exception {

        // GIVEN: existing company on platform
        NegotiationSettings negotiationSettings = this.initNegotiationSettings();

        // WHEN: setting update negotiation settings
        negotiationSettings.getWarrantyPeriodUnits().add("new unit");

        Gson gson = new Gson();
        this.mockMvc.perform(put("/company-settings/negotiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(negotiationSettings))
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // THEN: settings should updated
        this.mockMvc.perform(get("/company-settings/negotiation/" + negotiationSettings.getCompany().getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.warrantyPeriodUnits.length()", is(2)))
                .andExpect(jsonPath("$.warrantyPeriodUnits[0]", is("weeks")))
                .andExpect(jsonPath("$.warrantyPeriodUnits[1]", is("new unit")));
    }

    @Test
    public void testNoHeaderRequest() throws Exception {

        // GIVEN: on existing company on platform
        NegotiationSettings negotiationSettings = new NegotiationSettings();

        // WHEN: setting update negotiation settings
        Gson gson = new Gson();
        this.mockMvc.perform(put("/company-settings/negotiation/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(negotiationSettings))
                .accept(MediaType.APPLICATION_JSON))
                // THEN: 4xx error should occur
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testProfileCompleteness() throws Exception {

        // GIVEN: existing company on platform
        PartyType company = identityService.getCompanyOfUser(null).get();
        partyRepository.save(company);

        CompanySettings companySettings = new CompanySettings();

        Gson gson = new Gson();
        this.mockMvc.perform(put("/company-settings/" + company.getID())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .content(gson.toJson(companySettings)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        String responseAsString = this.mockMvc.perform(get("/company-settings/" + company.getID() + "/completeness"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        PartyType responseParty = objectMapper.readValue(responseAsString, PartyType.class);

        // check quality indicator which should all be zero
        QualityIndicatorType profileCompleteness = UblUtils.extractQualityIndicator(responseParty, PROFILE_COMPLETENESS).orElse(null);
        QualityIndicatorType descriptionCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_DESCRIPTION).orElse(null);
        QualityIndicatorType detailCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_GENERAL_DETAILS).orElse(null);
        QualityIndicatorType certificationCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_CERTIFICATE_DETAILS).orElse(null);
        QualityIndicatorType tradeCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_TRADE_DETAILS).orElse(null);

        assertFalse(profileCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertFalse(descriptionCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertFalse(detailCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertFalse(certificationCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertFalse(tradeCompleteness.getQuantity().getValue().doubleValue() > 0.0);

        // WHEN: updating company settings
        CompanyDetails companyDetails = new CompanyDetails();
        companyDetails.setCompanyLegalName("company name");
        companySettings.setDetails(companyDetails);
        CompanyDescription companyDescription = new CompanyDescription();
        companyDescription.setCompanyStatement("company statement");
        companySettings.setDescription(companyDescription);
        CompanyTradeDetails companyTradeDetails = new CompanyTradeDetails();
        companyTradeDetails.setPpapCompatibilityLevel(5);
        companySettings.setTradeDetails(companyTradeDetails);

        this.mockMvc.perform(put("/company-settings/" + company.getID())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .content(gson.toJson(companySettings)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        responseAsString = this.mockMvc.perform(get("/company-settings/" + company.getID() + "/completeness"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        responseParty = objectMapper.readValue(responseAsString, PartyType.class);

        // THEN: completeness indicators should be updated
        profileCompleteness = UblUtils.extractQualityIndicator(responseParty, PROFILE_COMPLETENESS).orElse(null);
        descriptionCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_DESCRIPTION).orElse(null);
        detailCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_GENERAL_DETAILS).orElse(null);
        certificationCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_CERTIFICATE_DETAILS).orElse(null);
        tradeCompleteness = UblUtils.extractQualityIndicator(responseParty, COMPLETENESS_OF_COMPANY_TRADE_DETAILS).orElse(null);

        assertTrue(profileCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertTrue(descriptionCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertTrue(detailCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertFalse(certificationCompleteness.getQuantity().getValue().doubleValue() > 0.0);
        assertTrue(tradeCompleteness.getQuantity().getValue().doubleValue() > 0.0);
    }

    @Test
    @Ignore
    public void testCertificateManagement() throws Exception {

        // GIVEN: existing company on platform
        PartyType company = identityService.getCompanyOfUser(null).get();
        partyRepository.save(company);
        company.setID(company.getHjid().toString());
        partyRepository.save(company);

        // upload certificate
        InputStream certContentStream = new ByteArrayInputStream("cert content".getBytes());
        MockMultipartFile certFile = new MockMultipartFile("file", "cert.txt", "multipart/form-data", certContentStream);
        this.mockMvc.perform(MockMvcRequestBuilders.fileUpload(String.format("/company-settings/certificate"))
                .file(certFile)
                .param("name", "cert name")
                .param("type", "cert type")
                .param("description", "cert description")
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN"))
                .andDo(print())
                .andExpect(status().isOk());

        // check if certificate is listed in company settings
        String responseAsString = this.mockMvc.perform(get("/company-settings/" + company.getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.certificates.length()", is(1)))
                .andExpect(jsonPath("$.certificates[0].name", is("cert name")))
                .andExpect(jsonPath("$.certificates[0].type", is("cert type")))
                .andExpect(jsonPath("$.certificates[0].description", is("cert description")))
                .andReturn().getResponse().getContentAsString();
        CompanySettings settings = objectMapper.readValue(responseAsString, CompanySettings.class);

        // download certificate
        String certId = settings.getCertificates().get(0).getId();
        byte[] certBytes = this.mockMvc.perform(get("/company-settings/certificate/" + certId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals("cert content", new String(certBytes, StandardCharsets.UTF_8));

        // delete certificate
        this.mockMvc.perform(delete("/company-settings/certificate/" + certId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN"))
                .andDo(print())
                .andExpect(status().isOk());

        // check for empty certificate list
        this.mockMvc.perform(get("/company-settings/" + company.getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.certificates.length()", is(0)));
    }

    public NegotiationSettings initNegotiationSettings() throws Exception {
        // GIVEN: existing company on platform
        PartyType company = identityService.getCompanyOfUser(null).get();
        partyRepository.save(company);
        company.setID(company.getHjid().toString());
        partyRepository.save(company);

        // WHEN: changing negotiation settings of company
        NegotiationSettings negotiationSettings = new NegotiationSettings();
        negotiationSettings.getWarrantyPeriodUnits().add("weeks");
        negotiationSettings.getWarrantyPeriodRanges().add(new NegotiationSettings.Range(5, 6));
        negotiationSettings.getDeliveryPeriodUnits().add("days");
        negotiationSettings.getDeliveryPeriodUnits().add("weeks");
        negotiationSettings.getDeliveryPeriodRanges().add(new NegotiationSettings.Range(1, 3));
        negotiationSettings.getDeliveryPeriodRanges().add(new NegotiationSettings.Range(2, 4));
        negotiationSettings.getIncoterms().add("inco_1");
        negotiationSettings.getIncoterms().add("inco_2");
        negotiationSettings.getPaymentMeans().add("pm_1");
        negotiationSettings.getPaymentTerms().add("pt_1");

        Gson gson = new Gson();
        this.mockMvc.perform(put("/company-settings/negotiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(negotiationSettings))
                .header(HttpHeaders.AUTHORIZATION, "Bearer DUMMY_TOKEN")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        negotiationSettings.setCompany(company);
        return negotiationSettings;
    }
}
