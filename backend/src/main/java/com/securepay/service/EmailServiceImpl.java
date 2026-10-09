package com.securepay.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Autowired
    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String username, String otpCode, String referenceCode, BigDecimal amount) {
        if (!mailEnabled) {
            throw new IllegalStateException("Email OTP delivery is disabled. Enable app.mail.enabled to send verification codes.");
        }

        String recipientEmail = toEmail != null ? toEmail.trim() : "";
        if (recipientEmail.isBlank()) {
            throw new IllegalArgumentException("A recipient email address is required.");
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("SMTP sender is not configured. Set SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD.");
        }

        try {
            String formattedAmount = amount != null ? "INR " + amount : "INR 0.00";
            sendHtmlOtpEmail(recipientEmail, username, otpCode, referenceCode, formattedAmount);
            logger.info("Sent transaction verification email for {} to {}", referenceCode, recipientEmail);
        } catch (MessagingException | MailException ex) {
            logger.warn("HTML OTP email delivery failed for {} to {} ({}: {}). Retrying with plain text.",
                    referenceCode, recipientEmail, ex.getClass().getSimpleName(), rootMessage(ex));
            try {
                sendPlainTextOtpEmail(recipientEmail, username, otpCode, referenceCode, amount);
                logger.info("Sent plain-text transaction verification email for {} to {}", referenceCode, recipientEmail);
            } catch (MessagingException | MailException retryEx) {
                logger.error("OTP email delivery failed for {} to {}: {}",
                        referenceCode, recipientEmail, rootMessage(retryEx));
                throw new IllegalStateException("Could not send the verification code by email. Check the SMTP configuration and try again.", retryEx);
            }
        }
    }

    private void sendHtmlOtpEmail(String toEmail, String username, String otpCode, String referenceCode, String formattedAmount)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail.trim());
        helper.setTo(toEmail);
        helper.setSubject("SecurePay: Your OTP for Transaction " + referenceCode);
        helper.setText(buildOtpHtmlTemplate(username, otpCode, referenceCode, formattedAmount), true);

        mailSender.send(message);
    }

    private void sendPlainTextOtpEmail(String toEmail, String username, String otpCode, String referenceCode, BigDecimal amount)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(fromEmail.trim());
        helper.setTo(toEmail);
        helper.setSubject("SecurePay: Your OTP for Transaction " + referenceCode);
        helper.setText("""
                Hello %s,

                Your SecurePay OTP is: %s

                Transaction reference: %s
                Transfer amount: INR %s
                Validity: 5 minutes

                Never share this OTP with anyone.
                """.formatted(
                username != null ? username : "User",
                otpCode != null ? otpCode : "",
                referenceCode != null ? referenceCode : "",
                amount != null ? amount : BigDecimal.ZERO
        ), false);

        mailSender.send(message);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : throwable.getMessage();
    }

    private String buildOtpHtmlTemplate(String username, String otpCode, String referenceCode, String amount) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: Arial, sans-serif; background-color: #0b1120; color: #f1f5f9; margin: 0; padding: 20px; }
                .container { max-width: 540px; margin: 0 auto; background-color: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 32px; }
                .brand { font-size: 24px; font-weight: bold; color: #38bdf8; }
                .title { font-size: 18px; font-weight: 600; color: #ffffff; margin-top: 20px; }
                .desc { font-size: 14px; color: #94a3b8; line-height: 1.5; margin: 12px 0 24px; }
                .otp-box { background: #1e293b; border: 2px dashed #38bdf8; border-radius: 8px; text-align: center; padding: 18px; margin: 20px 0; }
                .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #38bdf8; font-family: monospace; }
                .details { background: #1e293b; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 13px; color: #cbd5e1; }
                .details table { width: 100%; border-collapse: collapse; }
                .details td { padding: 6px 0; }
                .details td.label { color: #94a3b8; width: 45%; }
                .details td.value { font-weight: 600; color: #f8fafc; text-align: right; }
                .footer { font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid #334155; padding-top: 20px; margin-top: 28px; }
                .warning { color: #f87171; font-weight: 600; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="brand">SecurePay <span style="font-size:12px;color:#94a3b8;">FINTECH OS</span></div>
                <div class="title">Step-Up Transaction Verification</div>
                <div class="desc">
                  Hello <strong>{{USERNAME}}</strong>,<br>
                  A money transfer initiated on your account triggered a security verification. Use the one-time code below to authorize this transaction.
                </div>
                <div class="otp-box">
                  <div style="font-size: 11px; text-transform: uppercase; color: #94a3b8; letter-spacing: 1px; margin-bottom: 6px;">Your 6-Digit OTP</div>
                  <div class="otp-code">{{OTP_CODE}}</div>
                </div>
                <div class="details">
                  <table>
                    <tr><td class="label">Reference Code:</td><td class="value">{{REF_CODE}}</td></tr>
                    <tr><td class="label">Transfer Amount:</td><td class="value">{{AMOUNT}}</td></tr>
                    <tr><td class="label">Validity:</td><td class="value">5 Minutes</td></tr>
                  </table>
                </div>
                <div style="font-size: 12px; color: #cbd5e1; line-height: 1.5;">
                  <span class="warning">Security Alert:</span> Never share this OTP with anyone. If you did not initiate this transfer, please log in and change your password.
                </div>
                <div class="footer">
                  2026 SecurePay Inc. Real-time Fraud Detection and Adaptive Security Gateway.
                </div>
              </div>
            </body>
            </html>
            """;

        return template
                .replace("{{USERNAME}}", username != null ? username : "User")
                .replace("{{OTP_CODE}}", otpCode != null ? otpCode : "")
                .replace("{{REF_CODE}}", referenceCode != null ? referenceCode : "")
                .replace("{{AMOUNT}}", amount != null ? amount : "INR 0.00");
    }
}
