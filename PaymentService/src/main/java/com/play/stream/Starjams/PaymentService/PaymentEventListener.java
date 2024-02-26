package com.play.stream.Starjams.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class PaymentEventListener {
    private final StripeService stripeService;
    private final EmailService emailService;

    @Autowired
    public PaymentEventListener(StripeService stripeService, EmailService emailService) {
        this.stripeService = stripeService;
        this.emailService = emailService;
    }

    @EventListener
    public void onPaymentProcessEvent(PaymentProcessEvent event) {
        // Extract information from the event
        UUID transactionId = UUID.fromString(event.getTransactionId());
        String customerName = event.getCustomerName();
        String email = event.getEmail();

        // Log and perform business logic as before
        // Use the EmailService to send an email
        String emailContent = "Dear " + customerName + ", your payment with transaction ID " + transactionId + " has been processed successfully.";
        try {
            emailService.sendEmail(email, "Payment Confirmation", emailContent);
        } catch (Exception e) {
            // Handle exception, possibly log the error
            System.out.println("Error sending email: " + e.getMessage());
        }
    }
}
