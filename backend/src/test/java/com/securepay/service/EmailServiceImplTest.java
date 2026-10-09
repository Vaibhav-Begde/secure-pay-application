package com.securepay.service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "securepay@example.com");
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
    }

    @Test
    void sendOtpEmail_SendsCodeToRegisteredEmail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendOtpEmail(
                "alice@example.com",
                "alice",
                "654321",
                "TRX-TEST123",
                new BigDecimal("300.00")
        );

        verify(mailSender, timeout(3000)).send(message);
        assertEquals("alice@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        message.writeTo(content);
        assertTrue(content.toString(StandardCharsets.UTF_8).contains("654321"));
    }

    @Test
    void sendOtpEmail_WhenDeliveryFails_ReportsEmailFailure() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class, () -> emailService.sendOtpEmail(
                "alice@example.com",
                "alice",
                "654321",
                "TRX-TEST123",
                new BigDecimal("300.00")
        ));

        verify(mailSender, timeout(3000)).send(any(MimeMessage.class));
    }

    @Test
    void sendOtpEmail_WhenSenderNotConfigured_LogsAndDoesNotThrow() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        emailService.sendOtpEmail(
                "alice@example.com",
                "alice",
                "654321",
                "TRX-TEST123",
                new BigDecimal("300.00")
        );
    }
}
