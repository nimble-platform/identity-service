package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.entity.dto.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties.LanguageID.ENGLISH;

/**
 * Created by Johannes Innerbichler on 2019-02-01.
 */
public class TestUtils {

    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd";

    public static CompanyRegistration createCompanyRegistration(String legalName, PersonType user) {

        HashMap<NimbleConfigurationProperties.LanguageID, String> industrySectors = new HashMap<>();
        industrySectors.put(ENGLISH, "industrySector");

        HashMap<NimbleConfigurationProperties.LanguageID, String> companyStatement = new HashMap<>();
        companyStatement.put(ENGLISH, "industrySector");

        CompanyDetails companyDetails = new CompanyDetails(Collections.singletonMap(ENGLISH, "brand name"),
                Collections.singletonMap(ENGLISH, legalName), "vat number", "verification info",
                new Address(), "business type", Collections.singletonMap(ENGLISH, "business type"), 1970,
                industrySectors,new ArrayList<>());
        CompanyDescription companyDescription = new CompanyDescription(companyStatement, "website",
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
