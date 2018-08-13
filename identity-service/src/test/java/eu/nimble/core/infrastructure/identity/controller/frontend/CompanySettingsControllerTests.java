package eu.nimble.core.infrastructure.identity.controller.frontend;

import com.google.gson.Gson;
import eu.nimble.core.infrastructure.identity.IdentityServiceApplication;
import eu.nimble.core.infrastructure.identity.IdentityUtilsTestConfiguration;
import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;

/**
 * Created by Johannes Innerbichler on 09.08.18.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = IdentityServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder
@Import(IdentityUtilsTestConfiguration.class)
public class CompanySettingsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityUtils identityUtils;

    @Autowired
    private PartyRepository partyRepository;

    @Test
    @DirtiesContext
    public void testCreateCompanySettings() throws Exception {

        // GIVEN: existing company on platform
        PartyType company = identityUtils.getCompanyOfUser(null).get();
        partyRepository.save(company);

        // WHEN: updating company settings
        CompanySettings companySettings = new CompanySettings();
        companySettings.setName("company name");
        companySettings.setVatNumber("vat number");
        companySettings.setVerificationInformation("verification number");
        companySettings.setWebsite("website");
        companySettings.setAddress(new Address("street name", "building number", "city name", "postal code", "country"));
        companySettings.getPaymentMeans().add(new PaymentMeans("instruction note"));
        companySettings.getDeliveryTerms().add(new DeliveryTerms("special terms", new Address(), 5));
        companySettings.setPpapCompatibilityLevel(5);
        companySettings.getPreferredProductCategories().add("category 1");
        companySettings.getPreferredProductCategories().add("category 2");
        companySettings.getRecentlyUsedProductCategories().add("category 3");
        companySettings.getRecentlyUsedProductCategories().add("category 4");
        companySettings.getIndustrySectors().add("industry sector 1");
        companySettings.getIndustrySectors().add("industry sector 2");

        Gson gson = new Gson();
        this.mockMvc.perform(put("/company-settings/" + company.getID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(companySettings)))
                .andExpect(status().isAccepted());

        // THEN: getting settings should be updated
        this.mockMvc.perform(get("/company-settings/" + company.getID()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vatNumber", is("vat number")))
                .andExpect(jsonPath("$.verificationInformation", is("verification number")))
                .andExpect(jsonPath("$.website", is("website")))
                .andExpect(jsonPath("$.address.streetName", is("street name")))
                .andExpect(jsonPath("$.address.buildingNumber", is("building number")))
                .andExpect(jsonPath("$.address.cityName", is("city name")))
                .andExpect(jsonPath("$.address.postalCode", is("postal code")))
                .andExpect(jsonPath("$.address.country", is("country")))
                .andExpect(jsonPath("$.certificates.length()", is(0)))
                .andExpect(jsonPath("$.preferredProductCategories.length()", is(2)))
                .andExpect(jsonPath("$.paymentMeans.length()", is(1)))
                .andExpect(jsonPath("$.paymentMeans.[0].instructionNote", is("instruction note")))
                .andExpect(jsonPath("$.deliveryTerms.length()", is(1)))
                .andExpect(jsonPath("$.deliveryTerms[0].specialTerms", is("special terms")))
                .andExpect(jsonPath("$.deliveryTerms[0].estimatedDeliveryTime", is(5)))
                .andExpect(jsonPath("$.preferredProductCategories.length()", is(2)))
                .andExpect(jsonPath("$.preferredProductCategories", hasItem("category 1")))
                .andExpect(jsonPath("$.preferredProductCategories", hasItem("category 2")))
                .andExpect(jsonPath("$.recentlyUsedProductCategories.length()", is(2)))
                .andExpect(jsonPath("$.recentlyUsedProductCategories", hasItem("category 3")))
                .andExpect(jsonPath("$.recentlyUsedProductCategories", hasItem("category 4")))
                .andExpect(jsonPath("$.verificationInformation", is("verification number")))
                .andExpect(jsonPath("$.industrySectors.length()", is(2)))
                .andExpect(jsonPath("$.industrySectors[0]", is("industry sector 1")))
                .andExpect(jsonPath("$.industrySectors[1]", is("industry sector 2")));
    }

    @Test
    @DirtiesContext
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
    @DirtiesContext
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
    @DirtiesContext
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
    @DirtiesContext
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

    public NegotiationSettings initNegotiationSettings() throws Exception {
        // GIVEN: existing company on platform
        PartyType company = identityUtils.getCompanyOfUser(null).get();
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
