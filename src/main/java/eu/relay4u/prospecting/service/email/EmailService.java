package eu.relay4u.prospecting.service.email;

public interface EmailService {
    void sendVerificationCode(String toEmail, String recipientName, String code);
}
