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
        assertThat(saved.getCreatedAt().getNano() % 1_000_000).isZero();
    }

    @Test
    void listsPaymentsNewestFirst() {
        paymentRepository.save(newPayment("PAY-FFFFFFFFFFF1", "INV-2001"));
        paymentRepository.save(newPayment("PAY-FFFFFFFFFFF2", "INV-2002"));
        paymentRepository.save(newPayment("PAY-FFFFFFFFFFF3", "INV-2003"));

        assertThat(paymentRepository.findAllByOrderByCreatedAtDescIdDesc())
                .extracting(Payment::getMerchantReference)
                .containsExactly("INV-2003", "INV-2002", "INV-2001");
    }

    @Test
    void listsOnlyPaymentsWithGivenStatus() {
        paymentRepository.save(newPayment("PAY-GGGGGGGGGGG1", "INV-3001"));

        assertThat(paymentRepository.findAllByStatusOrderByCreatedAtDescIdDesc(PaymentStatus.PENDING)).hasSize(1);
        assertThat(paymentRepository.findAllByStatusOrderByCreatedAtDescIdDesc(PaymentStatus.FAILED)).isEmpty();
    }

    private Payment newPayment(String reference, String merchantReference) {
        return new Payment(reference, merchantReference, new BigDecimal("1500.00"), "KES", "254712345678");
    }
}
