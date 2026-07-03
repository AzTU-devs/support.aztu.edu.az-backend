package com.aztu.support.service;

import com.aztu.support.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends templated HTML emails asynchronously. All events use one reusable
 * template ({@code email/generic.html}); the caller supplies an {@link EmailContent}.
 * Failures are logged and never propagate back into the calling transaction.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final AppProperties props;

    public EmailService(JavaMailSender mailSender, ITemplateEngine templateEngine, AppProperties props) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.props = props;
    }

    @Async
    public void send(String to, String subject, EmailContent content) {
        if (!props.getMail().isEnabled()) {
            log.info("[MAIL DISABLED] Would send to='{}' subject='{}'", to, subject);
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("subject", subject);
            ctx.setVariable("heading", content.heading());
            ctx.setVariable("greeting", content.greeting());
            ctx.setVariable("paragraphs", content.paragraphs());
            ctx.setVariable("ctaLabel", content.ctaLabel());
            ctx.setVariable("ctaUrl", content.ctaUrl());
            ctx.setVariable("footerNote", content.footerNote());
            String html = templateEngine.process("email/generic", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(props.getMail().getFrom(), props.getMail().getFromName());
            mailSender.send(message);
            log.info("Sent email to '{}' subject='{}'", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to '{}' subject='{}': {}", to, subject, e.getMessage());
        }
    }
}
