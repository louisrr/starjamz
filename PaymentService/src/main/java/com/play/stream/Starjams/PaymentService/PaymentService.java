package com.play.stream.Starjams.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;

    @Autowired
    public PaymentService(PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public List<PaymentRecord> getPaymentsByStatus(String status) {
        return paymentRecordRepository.findByStatus(status);
    }
}
