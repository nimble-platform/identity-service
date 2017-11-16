package eu.nimble.core.infrastructure.identity.mail;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine textMailTemplateEngine;

    @Value("${spring.mail.defaultFrom}")
    private String defaultFrom;

    @Value("${nimble.frontend.url}")
    private String frontendUrl;

    public void sendInvite(String toEmail, String senderName, String companyName) throws UnsupportedEncodingException {

//        URIBuilder invitationUriBuilder = new URIBuilder(frontendUrl + "/#/registration/");
//        invitationUriBuilder.addParameter("email", toEmail);
//        String invitationUrl = invitationUriBuilder.build().toURL().toString();
        String invitationUrl = frontendUrl + "/#/registration/?email=" + URLEncoder.encode(toEmail, "UTF-8");

        Context context = new Context();
        context.setVariable("senderName", senderName);
        context.setVariable("companyName", companyName);
        context.setVariable("invitationUrl", invitationUrl);

        String subject = "Invitation to the NIMBLE platform";

        this.send(toEmail, subject, "invitation", context);
    }

    private void send(String to, String subject, String template, Context context) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        String message = this.textMailTemplateEngine.process(template, context);
        mailMessage.setFrom(this.defaultFrom);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        this.emailSender.send(mailMessage);
    }
}
