package com.olash.pipegate.merchant.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail,
                             String businessName,
                             String otp) {
        String subject = "Verify your Pipegate account";
        String content = buildOtpEmailContent(businessName, otp);
        sendEmail(toEmail, subject, content);
    }

    public void sendWelcomeEmail(String toEmail,
                                 String businessName,
                                 String merchantCode) {
        String subject = "Welcome to Pipegate — Your account is active";
        String content = buildWelcomeEmailContent(businessName, merchantCode);
        sendEmail(toEmail, subject, content);
    }

    private void sendEmail(String toEmail,
                           String subject,
                           String content) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom("noreply@pipegate.io");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);

            log.info("Email sent successfully. to={}, subject={}",
                    toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email. to={}, subject={}, error={}",
                    toEmail, subject, e.getMessage());

            throw new RuntimeException(
                    "Failed to send email to " + toEmail, e);
        }
    }

    private String buildOtpEmailContent(String businessName,
                                        String otp) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif;\s
                             color: #333;">
                    <h2>Verify your Pipegate account</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for registering on Pipegate.</p>
                    <p>Use the code below to verify\s
                       your email address:</p>
                    <div style="margin: 24px 0;">
                        <span style="font-size: 32px;\s
                                     font-weight: bold;\s
                                     letter-spacing: 8px;
                                     color: #1a1a2e;">%s</span>
                    </div>
                    <p>This code expires in\s
                       <strong>10 minutes.</strong></p>
                    <p>If you did not create a Pipegate account,\s
                       please ignore this email.</p>
                    <br/>
                    <p>The Pipegate Team</p>
                </body>
                </html>
               \s""".formatted(businessName, otp);
    }

    private String buildWelcomeEmailContent(String businessName,
                                            String merchantCode) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif;\s
                             color: #333;">
                    <h2>Welcome to Pipegate!</h2>
                    <p>Hello %s,</p>
                    <p>Your account has been verified\s
                       and is now active.</p>
                    <p>Your merchant code is:</p>
                    <div style="margin: 24px 0;">
                        <span style="font-size: 24px;\s
                                     font-weight: bold;
                                     color: #1a1a2e;">%s</span>
                    </div>
                    <p>Your API key and secret key were returned\s
                       to you after verification.</p>
                    <p style="color: #e74c3c;">
                        <strong>Important:</strong>\s
                        Store your secret key safely.\s
                        It will not be shown again.
                    </p>
                    <br/>
                    <p>The Pipegate Team</p>
                </body>
                </html>
               \s""".formatted(businessName, merchantCode);
    }
}
