package com.martinmaina.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;

public record PaymentResponse(
        String reference,
        String merchantReference,
        BigDecimal amount,
        String currency,
        String payerPhone,
        PaymentStatus status,
        String providerReference,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getReference(),
                payment.getMerchantReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPayerPhone(),
                payment.getStatus(),
                payment.getProviderReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
