package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.exception.DuplicatePaymentException;
import com.martinmaina.payments.repository.PaymentRepository;

@SpringBootTest
class PaymentServiceIntegrationTest {

    @MockitoSpyBean
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void simultaneousRequestWithSameDetailsReturnsFirstPayment() {
        saveFirstRequestWithoutBeingSeen();

        PaymentService.CreateResult result = paymentService.create(
                new CreatePaymentRequest("INV-2001", new BigDecimal("1500.0"), null, "254712345678"));

        assertThat(result.created()).isFalse();
        assertThat(result.payment().reference()).isEqualTo("PAY-FIRSTREQUEST");
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void simultaneousRequestWithDifferentDetailsIsRejected() {
        saveFirstRequestWithoutBeingSeen();

        assertThatThrownBy(() -> paymentService.create(
                new CreatePaymentRequest("INV-2001", new BigDecimal("2000.00"), null, "254712345678")))
                .isInstanceOf(DuplicatePaymentException.class);
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    // The first lookup misses the saved payment, as it would when two requests run at once
    private void saveFirstRequestWithoutBeingSeen() {
        paymentRepository.save(
                new Payment("PAY-FIRSTREQUEST", "INV-2001", new BigDecimal("1500.00"), "KES", "254712345678"));
        doReturn(Optional.empty())
                .doAnswer(invocation -> paymentRepository.findByReference("PAY-FIRSTREQUEST"))
                .when(paymentRepository).findByMerchantReference("INV-2001");
    }
}
