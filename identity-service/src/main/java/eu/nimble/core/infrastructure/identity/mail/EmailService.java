package eu.nimble.core.infrastructure.identity.mail;

import eu.nimble.core.infrastructure.identity.controller.frontend.CompanySettingsController;
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
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine textMailTemplateEngine;

    @Value("${spring.mail.defaultFrom}")
    private String defaultFrom;

    @Value("${spring.mail.debug:false}")
    private Boolean debug;

    @Value("${nimble.frontend.url}")
    private String frontendUrl;

    public void sendInvite(String toEmail, String senderName, String companyName) throws UnsupportedEncodingException {
        String invitationUrl = frontendUrl + "/#/user-mgmt/registration/?email=" + URLEncoder.encode(toEmail, "UTF-8");

        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("invitationUrl", invitationUrl);

        String subject = "Invitation to the NIMBLE platform";

        this.send(new String[]{toEmail}, subject, "invitation", context);
    }

    public void informInviteExistingCompany(String toEmail, String senderName, String companyName) {
        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("nimbleUrl", frontendUrl);

        String subject = "Invitation to " + companyName;

        this.send(new String[]{toEmail}, subject, "invitation_existing_company", context);
    }

    public void notifiyPlatformManagersNewCompany(List<String> emails, PersonType representative, PartyType company) {

        Context context = new Context();

        // collect info of user
        String username = representative.getFirstName() + " " + representative.getFamilyName();
        context.setVariable("username", username);

        String userEmail = null;
        if (representative.getContact() != null)
            userEmail = representative.getContact().getElectronicMail();
        context.setVariable("userEmail", userEmail);

        // collect info of user
        context.setVariable("companyName", company.getName());
        context.setVariable("companyID", company.getHjid());

        // collect info of company
        if (company.getPostalAddress() != null) {
            AddressType address = company.getPostalAddress();
            String countryName = address.getCountry() != null ? address.getCountry().getName() : null;
            context.setVariable("companyCountry", countryName);
            context.setVariable("companyStreet", address.getStreetName());
            context.setVariable("companyBuildingNumber", address.getBuildingNumber());
            context.setVariable("companyCity", address.getCityName());
            context.setVariable("companypostalCode", address.getPostalZone());
        }

        String subject = "NIMBLE: New company registered";

        this.send(emails.toArray(new String[]{}), subject, "new_company", context);
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
}
