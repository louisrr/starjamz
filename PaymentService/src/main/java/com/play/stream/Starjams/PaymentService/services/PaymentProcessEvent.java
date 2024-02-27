package com.play.stream.Starjams.PaymentService.services;

import org.springframework.context.ApplicationEvent;

public class PaymentProcessEvent extends ApplicationEvent {
    private final String transactionId; // Storing the transaction ID
    private final String customerName; // Storing the customer's name
    private final String email; // Storing the email address

    // Constructor that initializes the event object with additional information
    public PaymentProcessEvent(Object source, String transactionId, String customerName, String email) {
        super(source); // Call to the parent class (ApplicationEvent) constructor
        this.transactionId = transactionId;
        this.customerName = customerName;
        this.email = email;
    }

    // Getter for the transaction ID
    public String getTransactionId() {
        return transactionId;
    }

    // Getter for the customer name
    public String getCustomerName() {
        return customerName;
    }

    // Getter for the email
    public String getEmail() {
        return email;
    }

    // additional methods or fields
}
