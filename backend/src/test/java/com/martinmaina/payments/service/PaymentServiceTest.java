package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createsPendingPayment() {
        returnSavedPayment();

        PaymentService.CreateResult result = paymentService.create(request("KES"));

        PaymentResponse payment = result.payment();
        assertThat(result.created()).isTrue();
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.merchantReference()).isEqualTo("INV-1001");
        assertThat(payment.amount()).isEqualByComparingTo("1500.00");
        assertThat(payment.payerPhone()).isEqualTo("254712345678");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void defaultsCurrencyToKes() {
        returnSavedPayment();

        PaymentService.CreateResult result = paymentService.create(request(null));

        assertThat(result.payment().currency()).isEqualTo("KES");
    }

    @Test
    void keepsCurrencyFromRequest() {
        returnSavedPayment();

        PaymentService.CreateResult result = paymentService.create(request("UGX"));

        assertThat(result.payment().currency()).isEqualTo("UGX");
    }

    @Test
    void generatesReferenceInExpectedFormat() {
        returnSavedPayment();

        PaymentService.CreateResult result = paymentService.create(request("KES"));

        assertThat(result.payment().reference()).matches("PAY-[A-HJ-NP-Z2-9]{12}");
    }

    private void returnSavedPayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreatePaymentRequest request(String currency) {
        return new CreatePaymentRequest("INV-1001", new BigDecimal("1500.00"), currency, "254712345678");
    }
}
