package eu.nimble.core.infrastructure.identity.mail;

import eu.nimble.core.infrastructure.identity.controller.frontend.CompanySettingsController;
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

    public void notifiyPlatformManagersNewCompany(List<String> emails, String username, String comanyName) {

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("companyName", comanyName);

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
