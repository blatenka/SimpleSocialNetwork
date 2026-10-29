package com.aloute;

import com.aloute.service.MailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class MailServiceTest {

    @Test
    void passwordResetTemplateRendersSafeLinkAndVietnameseCopy() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("resetUrl", "https://aloute.example/reset?token=abc&source=mail");

        String html = engine.process("mail/password-reset", context);

        org.junit.jupiter.api.Assertions.assertTrue(html.contains("Đặt lại mật khẩu"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("token=abc&amp;source=mail"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("th:href"));
    }

    @Test
    void sendsUtf8PasswordResetEmail() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("mail/password-reset"), any(Context.class))).thenReturn("<html>Reset</html>");
        MailService service = new MailService(sender, "noreply@example.test", templateEngine);

        service.sendPasswordResetEmail("user@example.test",
                "https://aloute.example.test/reset-password?token=abc&source=mail");

        verify(sender).send(message);
        assertEquals("noreply@example.test", message.getFrom()[0].toString());
        assertEquals("user@example.test", message.getAllRecipients()[0].toString());
        assertEquals("Đặt lại mật khẩu ALOUTE", message.getSubject());
        verify(templateEngine).process(eq("mail/password-reset"), any(Context.class));
    }

    @Test
    void escapesUserProvidedTextBeforeSendingHtml() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        MailService service = new MailService(sender, "noreply@example.test", templateEngine);

        service.sendEmail("user@example.test", "Notice", "<script>alert(1)</script>");

        verify(sender).send(message);
        assertEquals("Notice", message.getSubject());
    }
}
