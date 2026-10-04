package com.martinmaina.payments.controller;

import java.net.URI;

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
import com.martinmaina.payments.dto.PageResponse;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.service.PaymentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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

    // The page limit keeps the row offset (page times size) inside the range the database query accepts
    @GetMapping
    public PageResponse<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return paymentService.list(status, page, size);
    }

    @GetMapping("/{reference}")
    public PaymentResponse get(@PathVariable String reference) {
        return paymentService.findByReference(reference);
    }
}
