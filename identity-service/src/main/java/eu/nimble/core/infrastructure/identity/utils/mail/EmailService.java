package eu.nimble.core.infrastructure.identity.utils.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine textMailTemplateEngine;

    @Value("${nimble.mail.default_from}")
    private String defaultFrom;

    public void sendInvite(String to) {
        Context context = new Context();
        context.setVariable("firstName", "Johannes");
        context.setVariable("lastName", "Innerbichler");
        context.setVariable("senderName", "Oliver Jung");
        context.setVariable("invitationUrl", "www.nimble-project.org/invite");

        String subject = "Invitation to the NIMBLE platform";

        this.send(to, subject, "invitation", context);
    }

    private void send(String to, String subject, String template, Context context) {
        String message = textMailTemplateEngine.process(template, context);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(defaultFrom);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        this.emailSender.send(mailMessage);
    }
}