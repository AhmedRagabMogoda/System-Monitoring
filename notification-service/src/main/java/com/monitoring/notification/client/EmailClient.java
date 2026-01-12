package com.monitoring.notification.client;


import com.monitoring.notification.model.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Client for sending HTML email notifications.
 * Uses Spring Mail with Thymeleaf templating for professional email formatting.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailClient {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    /**
     * Sends an email using the configured mail server.
     * Email content is generated from a Thymeleaf template.
     *
     * @param message the email message to send
     * @return Mono that completes when email is sent
     */
    public Mono<Void> sendEmail(EmailMessage message) {
        return Mono.fromCallable(() -> {
                    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setFrom(message.getFrom(), message.getFromName());
                    helper.setTo(message.getTo().toArray(new String[0]));

                    if (message.getCc() != null && !message.getCc().isEmpty()) {
                        helper.setCc(message.getCc().toArray(new String[0]));
                    }

                    helper.setSubject(message.getSubject());

                    String htmlContent = processTemplate(message.getTemplateName(),
                            message.getTemplateVariables());
                    helper.setText(htmlContent, true);

                    javaMailSender.send(mimeMessage);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnSuccess(v ->
                        log.debug("Email sent successfully: subject={}", message.getSubject()))
                .onErrorMap(e ->
                        new RuntimeException("Failed to send email: " + e.getMessage(), e));
    }

    /**
     * Processes a Thymeleaf template with provided variables.
     */
    private String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }
}
