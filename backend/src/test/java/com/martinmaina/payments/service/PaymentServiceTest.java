package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.dto.PageResponse;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.DuplicatePaymentException;
import com.martinmaina.payments.exception.PaymentNotFoundException;
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

    @Test
    void returnsExistingPaymentForRepeatedRequest() {
        Payment existing = existingPayment();
        when(paymentRepository.findByMerchantReference("INV-1001")).thenReturn(Optional.of(existing));

        PaymentService.CreateResult result = paymentService.create(
                new CreatePaymentRequest("INV-1001", new BigDecimal("1500.0"), null, "254712345678"));

        assertThat(result.created()).isFalse();
        assertThat(result.payment().reference()).isEqualTo("PAY-EXISTING0001");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void rejectsReusedMerchantReferenceWithDifferentAmount() {
        when(paymentRepository.findByMerchantReference("INV-1001")).thenReturn(Optional.of(existingPayment()));

        assertThatThrownBy(() -> paymentService.create(
                new CreatePaymentRequest("INV-1001", new BigDecimal("2000.00"), "KES", "254712345678")))
                .isInstanceOf(DuplicatePaymentException.class);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void returnsFirstPaymentWhenSimultaneousRequestIsSavedFirst() {
        when(paymentRepository.findByMerchantReference("INV-1001"))
                .thenReturn(Optional.empty(), Optional.of(existingPayment()));
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        PaymentService.CreateResult result = paymentService.create(request("KES"));

        assertThat(result.created()).isFalse();
        assertThat(result.payment().reference()).isEqualTo("PAY-EXISTING0001");
    }

    @Test
    void rejectsSimultaneousRequestWithDifferentDetails() {
        when(paymentRepository.findByMerchantReference("INV-1001"))
                .thenReturn(Optional.empty(), Optional.of(existingPayment()));
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> paymentService.create(
                new CreatePaymentRequest("INV-1001", new BigDecimal("2000.00"), "KES", "254712345678")))
                .isInstanceOf(DuplicatePaymentException.class);
    }

    @Test
    void rethrowsSaveErrorWhenNoPaymentWithMerchantReferenceExists() {
        when(paymentRepository.findByMerchantReference("INV-1001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("other"));

        assertThatThrownBy(() -> paymentService.create(request("KES")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsPaymentByReference() {
        when(paymentRepository.findByReference("PAY-EXISTING0001")).thenReturn(Optional.of(existingPayment()));

        PaymentResponse payment = paymentService.findByReference("PAY-EXISTING0001");

        assertThat(payment.merchantReference()).isEqualTo("INV-1001");
    }

    @Test
    void throwsWhenPaymentIsNotFound() {
        when(paymentRepository.findByReference("PAY-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findByReference("PAY-UNKNOWN"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment PAY-UNKNOWN was not found");
    }

    @Test
    void listsAllPaymentsNewestFirstWhenNoStatusIsGiven() {
        PageRequest expected = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(paymentRepository.findAll(expected))
                .thenReturn(new PageImpl<>(List.of(existingPayment()), expected, 11));

        PageResponse<PaymentResponse> page = paymentService.list(null, 1, 10);

        assertThat(page.content()).extracting(PaymentResponse::reference).containsExactly("PAY-EXISTING0001");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(paymentRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    void listsOnlyPaymentsWithGivenStatus() {
        PageRequest expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(paymentRepository.findAllByStatus(PaymentStatus.PENDING, expected))
                .thenReturn(new PageImpl<>(List.of(existingPayment()), expected, 1));

        PageResponse<PaymentResponse> page = paymentService.list(PaymentStatus.PENDING, 0, 10);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalPages()).isEqualTo(1);
        verify(paymentRepository, never()).findAll(any(Pageable.class));
    }

    private Payment existingPayment() {
        return new Payment("PAY-EXISTING0001", "INV-1001", new BigDecimal("1500.00"), "KES", "254712345678");
    }

    private void returnSavedPayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreatePaymentRequest request(String currency) {
        return new CreatePaymentRequest("INV-1001", new BigDecimal("1500.00"), currency, "254712345678");
    }
}
