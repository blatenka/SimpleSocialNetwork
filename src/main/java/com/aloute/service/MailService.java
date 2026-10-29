package com.aloute.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final TemplateEngine templateEngine;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from:no-reply@aloute.local}") String fromAddress,
                       TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.templateEngine = templateEngine;
    }

    public void sendEmail(String to, String subject, String text) throws MailException {
        send(to, subject, text, toHtml(text));
    }

    public void sendPasswordResetEmail(String to, String resetUrl) throws MailException {
        String subject = "Đặt lại mật khẩu ALOUTE";
        String text = "Xin chào,\n\nNhấn vào liên kết sau để đặt lại mật khẩu:\n"
                + resetUrl + "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.";
        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);
        String html = templateEngine.process("mail/password-reset", context);
        send(to, subject, text, html);
    }

    private void send(String to, String subject, String text, String html) throws MailException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not create email message", exception);
        }
    }

    private String toHtml(String text) {
        return "<div style=\"font-family:Arial,sans-serif;white-space:pre-wrap\">"
                + escapeHtml(text) + "</div>";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}