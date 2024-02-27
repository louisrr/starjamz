package com.play.stream.Starjams.PaymentService.services;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentRecordRepository extends CassandraRepository<PaymentRecord, UUID> {
    // This interface leverages Spring Data Cassandra
    List<PaymentRecord> findByStatus(String status);
    List<PaymentRecord> findByCreatedAtAfter(LocalDateTime dateTime);
    List<PaymentRecord> findByCustomerId(UUID customerId);
}