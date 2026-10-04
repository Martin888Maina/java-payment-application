package com.martinmaina.payments.dto;

import java.math.BigDecimal;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;

public record ReconciliationItem(
        String reference,
        BigDecimal ourAmount,
        BigDecimal providerAmount,
        PaymentStatus ourStatus,
        PaymentStatus providerStatus) {

    public static ReconciliationItem of(Payment payment, ProviderRecord record) {
        return new ReconciliationItem(payment.getReference(), payment.getAmount(), record.amount(),
                payment.getStatus(), record.result());
    }

    public static ReconciliationItem providerOnly(ProviderRecord record) {
        return new ReconciliationItem(record.reference(), null, record.amount(), null, record.result());
    }

    public static ReconciliationItem ourOnly(Payment payment) {
        return new ReconciliationItem(payment.getReference(), payment.getAmount(), null, payment.getStatus(), null);
    }
}
