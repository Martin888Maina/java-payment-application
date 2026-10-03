package com.martinmaina.payments.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.martinmaina.payments.exception.PaymentAlreadyFinalisedException;

class PaymentTest {

    @Test
    void newPaymentIsPending() {
        Payment payment = newPayment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.isFinal()).isFalse();
    }

    @Test
    void pendingPaymentCanSucceed() {
        Payment payment = newPayment();

        payment.complete(PaymentStatus.SUCCESSFUL, "QJK3H2L9P0");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        assertThat(payment.getProviderReference()).isEqualTo("QJK3H2L9P0");
        assertThat(payment.isFinal()).isTrue();
    }

    @Test
    void pendingPaymentCanFail() {
        Payment payment = newPayment();

        payment.complete(PaymentStatus.FAILED, "QJK3H2L9P1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.isFinal()).isTrue();
    }

    @Test
    void finishedPaymentCannotChange() {
        Payment payment = newPayment();
        payment.complete(PaymentStatus.SUCCESSFUL, "QJK3H2L9P0");

        assertThatThrownBy(() -> payment.complete(PaymentStatus.FAILED, "QJK3H2L9P2"))
                .isInstanceOf(PaymentAlreadyFinalisedException.class)
                .hasMessage("Payment PAY-AAAAAAAAAAAA is already SUCCESSFUL and cannot change");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        assertThat(payment.getProviderReference()).isEqualTo("QJK3H2L9P0");
    }

    @Test
    void paymentCannotBeCompletedAsPending() {
        Payment payment = newPayment();

        assertThatThrownBy(() -> payment.complete(PaymentStatus.PENDING, "QJK3H2L9P0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(payment.getProviderReference()).isNull();
    }

    private Payment newPayment() {
        return new Payment("PAY-AAAAAAAAAAAA", "INV-1001", new BigDecimal("1500.00"), "KES", "254712345678");
    }
}
