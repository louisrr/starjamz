package com.play.stream.Starjams.PaymentService;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.UUID;

@Table("payment_records")
public class PaymentRecord {

    @PrimaryKey
    private UUID transactionId; // Assuming transactionId is of type UUID

    private String status;

    // Constructors
    public PaymentRecord() {}

    public PaymentRecord(UUID transactionId, String status) {
        this.transactionId = transactionId;
        this.status = status;
    }

    // Getters and Setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
