package eu.nimble.core.infrastructure.identity.mail;

import com.google.common.base.Strings;
import eu.nimble.core.infrastructure.identity.config.message.NimbleMessageCode;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.AddressType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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

    @Value("${spring.mail.languages}")
    private String mailTemplateLanguages;

    public void sendResetCredentialsLink(String toEmail, String credentials, String language) throws UnsupportedEncodingException{
        String resetCredentialsURL = frontendUrl + "/#/user-mgmt/forgot/?key=" + URLEncoder.encode(credentials, "UTF-8");
        Context context = new Context();
        context.setVariable("resetPasswordURL", resetCredentialsURL);
        context.setVariable("platformName",platformName);

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_RESET_CREDENTIALS_LINK,language,Arrays.asList(platformName,version));

        this.send(new String[]{toEmail}, subject, getTemplateName("password-reset",language), context, new String[]{});
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

        this.send(new String[]{toEmail}, subject, getTemplateName("invitation",language), context, new String[]{supportEmail});
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

        this.send(new String[]{toEmail}, subject, getTemplateName("invitation_existing_company",language), context, new String[]{});
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
            String countryName = address.getCountry() != null ? address.getCountry().getName().getValue() : null;
            context.setVariable("companyCountry", countryName);
            context.setVariable("companyStreet", address.getStreetName());
            context.setVariable("companyBuildingNumber", address.getBuildingNumber());
            context.setVariable("companyCity", address.getCityName());
            context.setVariable("companypostalCode", address.getPostalZone());
        }

        String version = Strings.isNullOrEmpty(platformVersion) ? "": String.format(" (%s)",platformVersion);
        String subject = getMailSubject(NimbleMessageCode.MAIL_SUBJECT_COMPANY_REGISTERED, language, Arrays.asList(platformName,version));

        this.send(emails.toArray(new String[]{}), subject, getTemplateName("new_company",language), context, new String[]{});
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

        this.send(new String[]{email}, subject, getTemplateName("company_verified",language), context, new String[]{});
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
                this.send(new String[]{legalRepresentative.getContact().getElectronicMail()}, subject, getTemplateName("company_deleted",language), context, new String[]{});
            }catch (Exception e){
                logger.error("Failed to send email:",e);
            }
        }
    }

    private void send(String[] to, String subject, String template, Context context, String[] bcc) {

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

        if (bcc.length != 0) {
            mailMessage.setBcc(bcc);
        }

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
        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        String mailSubjectLanguage = languages.contains(language) ? language :languages.get(0) ;
        Locale locale = new Locale(mailSubjectLanguage);
        return this.messageSource.getMessage(messageCode.toString(), parameters.toArray(), locale);
    }
}
