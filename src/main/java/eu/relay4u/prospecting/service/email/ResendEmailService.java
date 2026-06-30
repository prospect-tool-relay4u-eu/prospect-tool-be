package eu.relay4u.prospecting.service.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResendEmailService implements EmailService {

    private final Resend resend;
    private final String fromEmail;

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendVerificationCode(String toEmail, String recipientName, String code) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Relay4U — Twój kod weryfikacyjny")
                .html(buildHtml(recipientName, code))
                .build();
        try {
            resend.emails().send(options);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", toEmail, e);
            throw new RuntimeException("Nie udało się wysłać emaila weryfikacyjnego. Spróbuj ponownie.", e);
        }
    }

    private String buildHtml(String name, String code) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2 style="color: #1a1a2e;">Witaj, %s!</h2>
                  <p>Twój kod weryfikacyjny do Relay4U:</p>
                  <div style="font-size: 36px; font-weight: bold; letter-spacing: 8px;
                              text-align: center; padding: 20px; background: #f4f4f8;
                              border-radius: 8px; margin: 20px 0;">
                    %s
                  </div>
                  <p style="color: #666;">Kod jest ważny przez <strong>15 minut</strong> i może być użyty tylko raz.</p>
                  <p style="color: #666;">Jeśli nie zakładałeś/aś konta w Relay4U, zignoruj tę wiadomość.</p>
                </div>
                """.formatted(name, code);
    }
}
