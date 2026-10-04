package com.martinmaina.payments.simulator;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.martinmaina.payments.repository.PaymentRepository;

// Clears test payments during local development. Real payment records are never deleted,
// so this endpoint only exists with the dev profile.
@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev/payments")
public class DemoDataController {

    private final PaymentRepository paymentRepository;

    public DemoDataController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        paymentRepository.deleteAllInBatch();
        return ResponseEntity.noContent().build();
    }
}
