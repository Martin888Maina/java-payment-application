package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.PaymentAlreadyFinalisedException;
import com.martinmaina.payments.exception.PaymentNotFoundException;
import com.martinmaina.payments.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    private static final String REFERENCE = "PAY-7F3K9Q2M8XWD";

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CallbackService callbackService;

    @Test
    void marksPendingPaymentAsSuccessful() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(payment)).thenReturn(payment);

        PaymentResponse response = callbackService.apply(callback("SUCCESSFUL"));

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESSFUL);
        assertThat(response.providerReference()).isEqualTo("QJK3H2L9P0");
        verify(paymentRepository).saveAndFlush(payment);
    }

    @Test
    void marksPendingPaymentAsFailed() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(payment)).thenReturn(payment);

        PaymentResponse response = callbackService.apply(callback("FAILED"));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void repeatedCallbackWithSameResultChangesNothing() {
        Payment payment = pendingPayment();
        payment.complete(PaymentStatus.SUCCESSFUL, "QJK3H2L9P0");
        when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(payment));

        PaymentResponse response = callbackService.apply(callback("SUCCESSFUL"));

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESSFUL);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void callbackWithDifferentResultIsRejected() {
        Payment payment = pendingPayment();
        payment.complete(PaymentStatus.SUCCESSFUL, "QJK3H2L9P0");
        when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> callbackService.apply(callback("FAILED")))
                .isInstanceOf(PaymentAlreadyFinalisedException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void callbackForUnknownPaymentThrows() {
        when(paymentRepository.findByReference(REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> callbackService.apply(callback("SUCCESSFUL")))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private CallbackRequest callback(String status) {
        return new CallbackRequest(REFERENCE, status, "QJK3H2L9P0");
    }

    private Payment pendingPayment() {
        return new Payment(REFERENCE, "INV-1001", new BigDecimal("1500.00"), "KES", "254712345678");
    }
}
