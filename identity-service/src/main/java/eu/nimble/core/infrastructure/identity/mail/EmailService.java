package eu.nimble.core.infrastructure.identity.mail;

import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.AddressType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@SuppressWarnings("Duplicates")
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private UblUtils ublUtils;

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

    public void sendResetCredentialsLink(String toEmail, String credentials) throws UnsupportedEncodingException{
        String resetCredentialsURL = frontendUrl + "/#/user-mgmt/forgot/?key=" + URLEncoder.encode(credentials, "UTF-8");
        Context context = new Context();
        context.setVariable("resetPasswordURL", resetCredentialsURL);

        String subject = "Reset Password to the NIMBLE platform";

        this.send(new String[]{toEmail}, subject, "password-reset", context, new String[]{});
    }

    public void sendInvite(String toEmail, String senderName, String companyName, Collection<String> roles) throws UnsupportedEncodingException {
        String invitationUrl = frontendUrl + "/#/user-mgmt/registration/?email=" + URLEncoder.encode(toEmail, "UTF-8");

        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("invitationUrl", invitationUrl);
        context.setVariable("roles", roles);

        String subject = "Invitation to the NIMBLE platform";

        this.send(new String[]{toEmail}, subject, "invitation", context, new String[]{supportEmail});
    }

    public void informInviteExistingCompany(String toEmail, String senderName, String companyName, Collection<String> roles) {
        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("nimbleUrl", frontendUrl);
        context.setVariable("roles", roles);

        String subject = "Invitation to " + companyName;

        this.send(new String[]{toEmail}, subject, "invitation_existing_company", context, new String[]{});
    }

    public void notifyPlatformManagersNewCompany(List<String> emails, PersonType representative, PartyType company) {

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

        String subject = "NIMBLE: New company registered";

        this.send(emails.toArray(new String[]{}), subject, "new_company", context, new String[]{});
    }

    public void notifyVerifiedCompany(String email, PersonType legalRepresentative, PartyType company) {

        Context context = new Context();
        context.setVariable("firstName", legalRepresentative.getFirstName());
        context.setVariable("familyName", legalRepresentative.getFamilyName());
        context.setVariable("companyName", ublUtils.getName(company));
        context.setVariable("supportEmail", supportEmail);

        String subject = "Your company has been verified on NIMBLE";

        this.send(new String[]{email}, subject, "company_verified", context, new String[]{});
    }

    private void send(String[] to, String subject, String template, Context context, String[] cc) {

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

        if (cc.length != 0) {
            mailMessage.setCc(cc);
        }

        this.emailSender.send(mailMessage);
    }
}
