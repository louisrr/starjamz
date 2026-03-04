package com.play.stream.Starjams.UserService.util;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;

/**
 * ConfirmationCode — interchangeable confirmation dispatch for email and SMS.
 *
 * Usage:
 *   String code = ConfirmationCode.generate();          // static — call anywhere
 *   confirmationCode.dispatch(contact, isEmail, code);  // injected bean — sends the code
 *
 * Email is sent via Amazon SES v2.
 * SMS   is sent via Twilio.
 */
@Service
public class ConfirmationCode {

    // 6-digit codes: 100_000–999_999 (always 6 digits, never needs zero-padding)
    private static final int CODE_MIN   = 100_000;
    private static final int CODE_RANGE = 900_000; // 999_999 - 100_000 + 1
    private static final SecureRandom RNG = new SecureRandom();

    @Value("${twilio.account-sid}")
    private String twilioSid;

    @Value("${twilio.auth-token}")
    private String twilioToken;

    @Value("${twilio.from-number}")
    private String twilioFrom;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.ses.from-email}")
    private String sesFromEmail;

    private SesV2Client sesClient;

    @PostConstruct
    private void init() {
        Twilio.init(twilioSid, twilioToken);
        sesClient = SesV2Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    // -------------------------------------------------------------------------
    // Code generation — static so it can be called without a Spring context
    // -------------------------------------------------------------------------

    /**
     * Generate a cryptographically random 6-digit confirmation code.
     *
     * @return e.g. "483921"
     */
    public static String generate() {
        return String.valueOf(CODE_MIN + RNG.nextInt(CODE_RANGE));
    }

    // -------------------------------------------------------------------------
    // Dispatch — routes to SES or Twilio based on contact type
    // -------------------------------------------------------------------------

    /**
     * Send {@code code} to the user's {@code contact}, using the appropriate
     * channel (email → SES, phone → Twilio).
     *
     * @param contact email address or E.164 phone number
     * @param isEmail true for email, false for SMS
     * @param code    value returned by {@link #generate()}
     */
    public void dispatch(String contact, boolean isEmail, String code) {
        if (isEmail) {
            sendEmail(contact, code);
        } else {
            sendSms(contact, code);
        }
    }

    // -------------------------------------------------------------------------
    // Internal senders
    // -------------------------------------------------------------------------

    private void sendEmail(String toAddress, String code) {
        String subject = "Your Starjamz confirmation code";
        String body    = "Your confirmation code is: " + code +
                         "\n\nThis code expires in 15 minutes.";

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(sesFromEmail)
                .destination(Destination.builder().toAddresses(toAddress).build())
                .content(EmailContent.builder()
                        .simple(software.amazon.awssdk.services.sesv2.model.Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(body).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }

    private void sendSms(String toNumber, String code) {
        Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(twilioFrom),
                "Your Starjamz confirmation code is: " + code + ". Expires in 15 minutes."
        ).create();
    }
}
