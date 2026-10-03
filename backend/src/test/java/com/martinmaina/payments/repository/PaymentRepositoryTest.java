package com.martinmaina.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void savesAndFindsPaymentByReference() {
        paymentRepository.save(newPayment("PAY-AAAAAAAAAAAA", "INV-1001"));

        Payment found = paymentRepository.findByReference("PAY-AAAAAAAAAAAA").orElseThrow();

        assertThat(found.getMerchantReference()).isEqualTo("INV-1001");
        assertThat(found.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void findsPaymentByMerchantReference() {
        paymentRepository.save(newPayment("PAY-BBBBBBBBBBBB", "INV-1002"));

        assertThat(paymentRepository.findByMerchantReference("INV-1002"))
                .map(Payment::getReference)
                .contains("PAY-BBBBBBBBBBBB");
    }

    @Test
    void returnsEmptyForUnknownReference() {
        assertThat(paymentRepository.findByReference("PAY-UNKNOWN")).isEmpty();
    }

    @Test
    void rejectsDuplicateMerchantReference() {
        paymentRepository.saveAndFlush(newPayment("PAY-CCCCCCCCCCCC", "INV-1003"));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(newPayment("PAY-DDDDDDDDDDDD", "INV-1003")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void setsTimestampsOnSave() {
        Payment saved = paymentRepository.saveAndFlush(newPayment("PAY-EEEEEEEEEEEE", "INV-1004"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    private Payment newPayment(String reference, String merchantReference) {
        return new Payment(reference, merchantReference, new BigDecimal("1500.00"), "KES", "254712345678");
    }
}
