package com.martinmaina.payments.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentService.CreateResult result = paymentService.create(request);
        PaymentResponse payment = result.payment();

        if (!result.created()) {
            return ResponseEntity.ok(payment);
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{reference}")
                .buildAndExpand(payment.reference())
                .toUri();
        return ResponseEntity.created(location).body(payment);
    }

    @GetMapping
    public List<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.list(status);
    }

    @GetMapping("/{reference}")
    public PaymentResponse get(@PathVariable String reference) {
        return paymentService.findByReference(reference);
    }
}
