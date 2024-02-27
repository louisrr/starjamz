package com.play.stream.Starjams.PaymentService.services;

import com.play.stream.Starjams.PaymentService.services.PaymentRecord;
import com.play.stream.Starjams.PaymentService.services.PaymentRecordRepository;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class StripeService {

    public StripeService(PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
    }

    private final PaymentRecordRepository paymentRecordRepository;

    private static final String SENDGRID_API_KEY = System.getenv("SENDGRID_API_KEY"); // Ensure this environment variable is set

    @Value("${stripe.api.key}")
    private String apiKey;

    public StripeService(@Value("${stripe.api.key}") String apiKey, PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        // Initialize Stripe API key
        Stripe.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        // Initialize Stripe API key
        Stripe.apiKey = apiKey;
    }


    public PaymentIntent processPayment(String amount, String currency, String paymentMethodId) throws StripeException {
        // Initialize Stripe with API key
        Stripe.apiKey = apiKey;

        // Convert the amount to a long (Stripe expects amounts to be in the smallest currency unit,
        // e.g., cents for USD)
        long amountInCents = Long.parseLong(amount);

        // Create the PaymentIntent parameters
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setPaymentMethod(paymentMethodId)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                .setConfirm(true) // Immediately confirm the payment
                .build();

        // Create and confirm the PaymentIntent
        return PaymentIntent.create(params);
    }

    public Subscription createSubscription(String customerId, String priceId) throws StripeException {
        // Prepare subscription parameters
        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                        SubscriptionCreateParams.Item.builder()
                                .setPrice(priceId) // Set the price ID, not the plan ID directly
                                .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.ERROR_IF_INCOMPLETE)
                .build();

        // Create subscription
        return Subscription.create(params);
    }

    public Subscription getSubscription(String subscriptionId) throws StripeException {
        // Retrieve the subscription by its ID
        return Subscription.retrieve(subscriptionId);
    }

    public Subscription updateSubscription(String subscriptionId, Map<String, Object> updates) throws StripeException {
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY");

        SubscriptionUpdateParams.Builder updateParamsBuilder = SubscriptionUpdateParams.builder();

        // Add all updates from the map to the SubscriptionUpdateParams
        // This assumes the updates map keys correspond to SubscriptionUpdateParams fields
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
        }
        SubscriptionUpdateParams updateParams = updateParamsBuilder.build();

        return Subscription.retrieve(subscriptionId).update(updateParams);
    }

    public Subscription cancelSubscription(String subscriptionId, boolean end) throws StripeException {
        // Set Stripe secret key. Preferably, retrieve it from a secure location
        // like an environment variable or configuration property.
        Stripe.apiKey = "sk_test_4eC39HqLyjWDarjtT1zdp7dc"; // Replace this with an actual Stripe API key

        // Cancel the subscription
        Subscription subscription = Subscription.retrieve(subscriptionId);
        Subscription canceledSubscription = subscription.cancel();

        return canceledSubscription;
    }

    // Attach a payment method to a customer
    public PaymentMethod attachPaymentMethodToCustomer(String paymentMethodId, String customerId) throws StripeException {
        // Initialize Stripe API key
        Stripe.apiKey = "sk_test_YourStripeSecretKey"; // Replace with your actual Stripe secret key

        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build();

        // Attach the payment method to the customer
        return paymentMethod.attach(params);
    }

    // Detach a payment method from a customer
    public PaymentMethod detachPaymentMethodFromCustomer(String paymentMethodId) throws StripeException {
        // Initialize Stripe API key
        Stripe.apiKey = "sk_test_YourStripeSecretKey"; // Replace with your actual Stripe secret key

        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        // Detach the payment method from the customer
        return paymentMethod.detach();
    }

    // Replace with your endpoint's secret from the Stripe dashboard
    private static final String endpointSecret = "whsec_YourEndpointSecret";

    public void handleWebhookEvent(String payload, String sigHeader) throws StripeException {
        try {
            // Verify the signature
            Event event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
            );

            // Handle the event
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    // Handle successful payment intent
                    // Cast the event data to the appropriate event object
                    System.out.println("PaymentIntent was successful!");
                    break;
                case "customer.subscription.created":
                    // Handle subscription creation
                    System.out.println("Subscription was created!");
                    break;
                // Add more cases for other event types you care about
                default:
                    System.out.println("Unhandled event type: " + event.getType());
            }
        } catch (SignatureVerificationException e) {
            // Invalid signature, throw an exception or handle accordingly
            throw new RuntimeException("Webhook error while validating signature.", e);
        }
    }

    @Transactional
    public void updatePaymentStatus(UUID transactionId, String status) {
        PaymentRecord record = paymentRecordRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment record with transaction ID " + transactionId + " not found"));

        record.setStatus(status);
        paymentRecordRepository.save(record);
    }

    public void processPaymentAndSendEmail(String email, String message) {
        Email from = new Email("email@starjamz.com"); // Replace with your "from" email address
        String subject = "Payment Processed"; // Customize this
        Email to = new Email(email); // The recipient's email address
        Content content = new Content("text/plain", message); // The email content
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(SENDGRID_API_KEY);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            System.out.println("Error sending email: " + ex.getMessage());
            // Handle the error appropriately in your application
        }
    }
}
