package com.foodya.backend.infrastructure.adapter.integration;

import com.foodya.backend.application.ports.out.OtpDeliveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SmtpOtpDeliveryAdapter implements OtpDeliveryPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpOtpDeliveryAdapter.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpOtpDeliveryAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
                                  @Value("${foodya.auth.otp.mail.from:no-reply@foodya.local}") String fromEmail) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured; skipped password reset OTP email for {}", email);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Foodya password reset OTP");
        message.setText("Your Foodya OTP is: " + otp + "\nIt expires in a few minutes. If you did not request this, ignore this email.");
        mailSender.send(message);
        log.info("Password reset OTP sent to {}", email);
    }
}
