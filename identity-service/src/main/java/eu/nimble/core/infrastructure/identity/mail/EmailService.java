package eu.nimble.core.infrastructure.identity.mail;

import com.google.common.base.Strings;
import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.config.message.NimbleMessageCode;
import eu.nimble.core.infrastructure.identity.entity.CompanyDetailsUpdates;
import eu.nimble.core.infrastructure.identity.mail.model.SubscriptionMailModel;
import eu.nimble.core.infrastructure.identity.mail.model.SubscriptionSummary;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.AddressType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.country.CountryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service
@SuppressWarnings("Duplicates")
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private UblUtils ublUtils;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private TemplateEngine textMailTemplateEngine;

    @Value("${spring.mail.defaultFrom}")
    private String defaultFrom;

    @Value("${spring.mail.debug:false}")
    private Boolean debug;

    @Value("${nimble.frontend.url}")
    private String frontendUrl;

    @Value("${nimble.supportEmail}")
    private String supportEmail;

    @Value("${nimble.platformName}")
    private String platformVersion;

    @Value("${spring.mail.platformName}")
    private String platformName;

    @Value("${nimble.frontend.registration.url}")
    private String frontendRegistrationUrl;

    @Value("${nimble.frontend.company-details.url}")
    private String companyDetailsUrl;

    @Value("${spring.mail.languages}")
    private String mailTemplateLanguages;

    @Value("${nimble.companyDataUpdateEmail}")
    private String companyDataUpdateEmail;

    public void sendResetCredentialsLink(String toEmail, String credentials, String language) throws UnsupportedEncodingException{
        String resetCredentialsURL = frontendUrl + "/#/user-mgmt/forgot/?key=" + URLEncoder.encode(credentials, "UTF-8");
        Context context = new Context();
        context.setVariable("resetPasswordURL", resetCredentialsURL);
        context.setVariable("platformName",platformName);

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_RESET_CREDENTIALS_LINK,language,Arrays.asList(platformName,version));

        this.send(new String[]{toEmail}, subject, getTemplateName("password-reset",language), context);
    }

    public void sendInvite(String toEmail, String senderName, String companyName, Collection<String> roles, String language) throws UnsupportedEncodingException {
        String invitationUrl = String.format("%s/%s/?email=%s",frontendUrl,frontendRegistrationUrl,URLEncoder.encode(toEmail, "UTF-8"));

        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("invitationUrl", invitationUrl);
        context.setVariable("roles", roles);
        context.setVariable("platformName",platformName);

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_INVITATION,language,Arrays.asList(platformName,version));

        this.send(new String[]{toEmail}, subject, getTemplateName("invitation",language), context);
    }

    public void sendSubscriptionSummary(List<String> toEmail, List<SubscriptionSummary> subscriptionSummaryList, String language) throws UnsupportedEncodingException {
        Context context = new Context();

        // keeps the list of SubscriptionMailModel which corresponds to the 'subscriptions' variable in mail template
        List<SubscriptionMailModel> subscriptionMailModels = new ArrayList<>();
        // populate subscriptionMailModels array
        for (SubscriptionSummary subscriptionSummary : subscriptionSummaryList) {
            // retrieve the urls for product details
            List<String> productDetailsUrls = new ArrayList<>();
            for (int i = 0; i < subscriptionSummary.getCatalogueIds().size(); i++) {
                productDetailsUrls.add(String.format("%s/#/product-details?catalogueId=%s&id=%s",frontendUrl,URLEncoder.encode(subscriptionSummary.getCatalogueIds().get(i), "UTF-8"),
                        URLEncoder.encode(subscriptionSummary.getProductIds().get(i), "UTF-8")));
            }
            // set title of subscription
            String title;
            // company
            if(subscriptionSummary.getCompanyName() != null){
                title = String.format("The following products are published/updated by %s",subscriptionSummary.getCompanyName());
            }
            // category
            else{
                title = String.format("The following products are published/updated on category: %s",subscriptionSummary.getCategoryName());
            }
            // create the subscription mail model
            SubscriptionMailModel subscriptionMailModel = new SubscriptionMailModel();
            subscriptionMailModel.setTitle(title);
            subscriptionMailModel.setProductUrls(productDetailsUrls);

            subscriptionMailModels.add(subscriptionMailModel);
        }
        // set context variables
        context.setVariable("subscriptions", subscriptionMailModels);
        context.setVariable("platformName",platformName);
        // set mail subject
        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_SUBSCRIPTION,language,Arrays.asList(platformName,version));

        this.send(toEmail.toArray(new String[0]), subject, getTemplateName("subscription",language), context);
    }

    public void informInviteExistingCompany(String toEmail, String senderName, String companyName, Collection<String> roles, String language) {
        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("nimbleUrl", frontendUrl);
        context.setVariable("roles", roles);
        context.setVariable("platformName",platformName);

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_INVITATION_EXISTING_COMPANY, language, Arrays.asList(companyName,platformName,version));

        this.send(new String[]{toEmail}, subject, getTemplateName("invitation_existing_company",language), context);
    }

    public void notifyPlatformManagersCompanyDataUpdates(List<String> emails, PersonType user, PartyType company, CompanyDetailsUpdates companyDetailsUpdates, String language){
        Context context = new Context();

        // collect info of user
        String username = user.getFirstName() + " " + user.getFamilyName();
        context.setVariable("username", username);

        // collect info of company and platform
        context.setVariable("companyName", ublUtils.getName(company));
        context.setVariable("platformName",platformName);
        // company updates
        context.setVariable("companyUpdates",getMailContentForCompanyDetailsUpdates(companyDetailsUpdates,language));
        // link to the company details
        context.setVariable("companyDetailsUrl", String.format("%s/%s?id=%s&viewMode=mgmt",frontendUrl,companyDetailsUrl,company.getPartyIdentification().get(0).getID()));

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_COMPANY_DATA_UPDATED, language, Arrays.asList(platformName,version));

        // if the specific email address is defined for the company data updates, use it to send the email
        // otherwise, send the email to all platform managers
        if(!Strings.isNullOrEmpty(this.companyDataUpdateEmail)){
            emails = Arrays.asList(this.companyDataUpdateEmail);
        }

        this.send(emails.toArray(new String[]{}), subject, getTemplateName("company_data_updated",language), context);
    }

    public void notifyPlatformManagersNewCompany(List<String> emails, PersonType representative, PartyType company, String language) {

        Context context = new Context();

        // collect info of user
        String username = representative.getFirstName() + " " + representative.getFamilyName();
        context.setVariable("username", username);

        String userEmail = null;
        if (representative.getContact() != null)
            userEmail = representative.getContact().getElectronicMail();
        context.setVariable("userEmail", userEmail);

        // collect info of user
        context.setVariable("companyName", ublUtils.getName(company));
        context.setVariable("companyID", company.getHjid());
        context.setVariable("platformName",platformName);

        // collect info of company
        if (company.getPostalAddress() != null) {
            AddressType address = company.getPostalAddress();
            String countryName = address.getCountry() != null && address.getCountry().getIdentificationCode() != null? CountryUtil.getCountryNameByISOCode(address.getCountry().getIdentificationCode().getValue()) : null;
            context.setVariable("companyCountry", countryName);
            context.setVariable("companyStreet", address.getStreetName());
            context.setVariable("companyBuildingNumber", address.getBuildingNumber());
            context.setVariable("companyCity", address.getCityName());
            context.setVariable("companypostalCode", address.getPostalZone());
        }

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_COMPANY_REGISTERED, language, Arrays.asList(platformName,version));

        this.send(emails.toArray(new String[]{}), subject, getTemplateName("new_company",language), context);
    }

    public void notifyVerifiedCompany(String email, PersonType legalRepresentative, PartyType company, String language) {

        Context context = new Context();
        context.setVariable("firstName", legalRepresentative.getFirstName());
        context.setVariable("familyName", legalRepresentative.getFamilyName());
        context.setVariable("companyName", ublUtils.getName(company));
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("nimbleUrl", frontendUrl);
        context.setVariable("platformName",platformName);

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_COMPANY_VERIFIED,language,Arrays.asList(platformName,version));

        this.send(new String[]{email}, subject, getTemplateName("company_verified",language), context);
    }

    public void notifyDeletedCompany(List<PersonType> legalRepresentatives, PartyType company, String language) {

        String companyName = ublUtils.getName(company);
        for (PersonType legalRepresentative : legalRepresentatives) {
            Context context = new Context();
            context.setVariable("firstName", legalRepresentative.getFirstName());
            context.setVariable("familyName", legalRepresentative.getFamilyName());
            context.setVariable("companyName", companyName);
            context.setVariable("platformName",platformName);

            String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
            String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_COMPANY_DELETED,language,Arrays.asList(platformName,version));

            try {
                this.send(new String[]{legalRepresentative.getContact().getElectronicMail()}, subject, getTemplateName("company_deleted",language), context);
            }catch (Exception e){
                logger.error("Failed to send email:",e);
            }
        }
    }

    private void send(String[] to, String subject, String template, Context context) {

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        String message = this.textMailTemplateEngine.process(template, context);

        if (debug) {
            logger.info(message);
            return;
        }

        mailMessage.setFrom(this.defaultFrom);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        this.emailSender.send(mailMessage);
    }

    private String getTemplateName(String templateName,String language){
        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        if(languages.contains(language)){
            return String.format("%s_%s",templateName,language);
        }
        return String.format("%s_%s",templateName,languages.get(0));
    }

    private String getMailSubject(NimbleMessageCode messageCode, String language, List<String> parameters){
        return this.messageSource.getMessage(messageCode.toString(), parameters.toArray(), getLocale(language));
    }

    /**
     * Returns the {{@link Locale}} for the given language
     * */
    private Locale getLocale(String language){
        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        String mailSubjectLanguage = languages.contains(language) ? language :languages.get(0) ;
        return new Locale(mailSubjectLanguage);
    }

    /**
     * Returns the mail content for company details updates
     * @param companyDetailsUpdates the updated fields of company details
     * @param language the language id to be used to put proper translations of company fields
     * @return the mail content for company details updates as string
     * */
    private String getMailContentForCompanyDetailsUpdates(CompanyDetailsUpdates companyDetailsUpdates,String language){
        Locale locale = getLocale(language);

        StringBuilder content = new StringBuilder();
        // company name
        if(companyDetailsUpdates.getLegalName() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.COMPANY_NAME.toString(),null,locale)).append(":\n");
            content.append(stringifyLanguageMap(companyDetailsUpdates.getLegalName())).append("\n");
        }
        // brand name
        if(companyDetailsUpdates.getBrandName() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.BRAND_NAME.toString(),null,locale)).append(":\n");
            content.append(stringifyLanguageMap(companyDetailsUpdates.getBrandName())).append("\n");
        }
        // vat number
        if(companyDetailsUpdates.getVatNumber() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.VAT_NUMBER.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getVatNumber()).append("\n\n");
        }
        // verification info
        if(companyDetailsUpdates.getVerificationInformation() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.VERIFICATION_INFO.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getVerificationInformation()).append("\n\n");
        }
        // business type
        if(companyDetailsUpdates.getBusinessType() != null){
            String businessTypeKey = companyDetailsUpdates.getBusinessType().replace(" ","_").toUpperCase();
            content.append(this.messageSource.getMessage(NimbleMessageCode.BUSINESS_TYPE.toString(),null,locale)).append(":\n");
            content.append(this.messageSource.getMessage(businessTypeKey,null,locale)).append("\n\n");
        }
        // activity sectors
        if(companyDetailsUpdates.getIndustrySectors() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.ACTIVITY_SECTORS.toString(),null,locale)).append(":\n");
            content.append(stringifyLanguageMap(companyDetailsUpdates.getIndustrySectors())).append("\n");
        }
        // business keywords
        if(companyDetailsUpdates.getBusinessKeywords() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.BUSINESS_KEYWORDS.toString(),null,locale)).append(":\n");
            content.append(stringifyLanguageMap(companyDetailsUpdates.getBusinessKeywords())).append("\n");
        }
        // year of foundation
        if(companyDetailsUpdates.getYearOfCompanyRegistration() != null){
            content.append(this.messageSource.getMessage(NimbleMessageCode.YEAR_FOUNDATION.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getYearOfCompanyRegistration()).append("\n\n");
        }
        // address
        if(companyDetailsUpdates.getAddress() != null){
            // street
            content.append(this.messageSource.getMessage(NimbleMessageCode.STREET.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getAddress().getStreetName()).append("\n\n");
            // building number
            content.append(this.messageSource.getMessage(NimbleMessageCode.BUILDING_NUMBER.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getAddress().getBuildingNumber()).append("\n\n");
            // city/town
            content.append(this.messageSource.getMessage(NimbleMessageCode.CITY_TOWN.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getAddress().getCityName()).append("\n\n");
            // state/province
            content.append(this.messageSource.getMessage(NimbleMessageCode.STATE_PROVINCE.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getAddress().getRegion()).append("\n\n");
            // postal code
            content.append(this.messageSource.getMessage(NimbleMessageCode.POSTAL_CODE.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getAddress().getPostalCode()).append("\n\n");
            // country
            content.append(this.messageSource.getMessage(NimbleMessageCode.COUNTRY.toString(),null,locale)).append(":\n");
            content.append(companyDetailsUpdates.getAddress().getCountry()).append("\n");
        }
        return content.toString();
    }

    /**
     * Stringifies the given language map
     * @param languageMap the language id - value map
     * @return language id - value pairs separeted by new line
     * */
    private String stringifyLanguageMap(Map<NimbleConfigurationProperties.LanguageID, String> languageMap){
        String newLineChar = "\n";
        StringBuilder stringBuilder = new StringBuilder();
        languageMap.forEach((languageID, value) -> {
            List<String> values = Arrays.asList(value.split(newLineChar).clone());
            values.forEach(v -> stringBuilder.append(v).append("(").append(languageID).append(")\n"));
        });
        return stringBuilder.toString();
    }
}
