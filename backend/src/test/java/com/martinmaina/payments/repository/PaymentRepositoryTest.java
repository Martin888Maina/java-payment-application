package com.martinmaina.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;

@DataJpaTest
class PaymentRepositoryTest {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

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
    void pagesPaymentsNewestFirst() {
        for (int i = 1; i <= 12; i++) {
            paymentRepository.save(newPayment("PAY-FFFFFFFFF%03d".formatted(i), "INV-2%03d".formatted(i)));
        }

        Page<Payment> secondPage = paymentRepository.findAll(PageRequest.of(1, 5, NEWEST_FIRST));

        assertThat(secondPage.getContent())
                .extracting(Payment::getMerchantReference)
                .containsExactly("INV-2007", "INV-2006", "INV-2005", "INV-2004", "INV-2003");
        assertThat(secondPage.getTotalElements()).isEqualTo(12);
        assertThat(secondPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    void pagesOnlyPaymentsWithGivenStatus() {
        paymentRepository.save(newPayment("PAY-GGGGGGGGGGG1", "INV-3001"));

        PageRequest firstPage = PageRequest.of(0, 10, NEWEST_FIRST);
        assertThat(paymentRepository.findAllByStatus(PaymentStatus.PENDING, firstPage).getTotalElements())
                .isEqualTo(1);
        assertThat(paymentRepository.findAllByStatus(PaymentStatus.FAILED, firstPage)).isEmpty();
    }

    private Payment newPayment(String reference, String merchantReference) {
        return new Payment(reference, merchantReference, new BigDecimal("1500.00"), "KES", "254712345678");
    }
}
