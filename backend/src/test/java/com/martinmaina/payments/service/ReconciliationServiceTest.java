package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.martinmaina.payments.dto.ProviderRecord;
import com.martinmaina.payments.dto.ReconciliationItem;
import com.martinmaina.payments.dto.ReconciliationRequest;
import com.martinmaina.payments.dto.ReconciliationResult;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.DuplicateProviderRecordException;
import com.martinmaina.payments.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void matchesRecordWithSameAmountAndStatus() {
        givenOurPayments(payment("PAY-AAA111AAA111", "1500.00", PaymentStatus.SUCCESSFUL));

        ReconciliationResult result = reconcile(record("PAY-AAA111AAA111", "1500.00", "SUCCESSFUL"));

        assertThat(result.matched()).extracting(ReconciliationItem::reference).containsExactly("PAY-AAA111AAA111");
        assertThat(result.amountMismatches()).isEmpty();
        assertThat(result.statusMismatches()).isEmpty();
    }

    @Test
    void comparesAmountsByValue() {
        givenOurPayments(payment("PAY-AAA111AAA111", "1500.00", PaymentStatus.SUCCESSFUL));

        ReconciliationResult result = reconcile(record("PAY-AAA111AAA111", "1500.0", "SUCCESSFUL"));

        assertThat(result.matched()).hasSize(1);
        assertThat(result.amountMismatches()).isEmpty();
    }

    @Test
    void reportsAmountMismatch() {
        givenOurPayments(payment("PAY-BBB222BBB222", "1000.00", PaymentStatus.SUCCESSFUL));

        ReconciliationResult result = reconcile(record("PAY-BBB222BBB222", "900.00", "SUCCESSFUL"));

        assertThat(result.amountMismatches()).singleElement().satisfies(item -> {
            assertThat(item.ourAmount()).isEqualByComparingTo("1000.00");
            assertThat(item.providerAmount()).isEqualByComparingTo("900.00");
        });
        assertThat(result.matched()).isEmpty();
    }

    @Test
    void reportsStatusMismatchWhenOurPaymentIsStillPending() {
        givenOurPayments(payment("PAY-CCC333CCC333", "250.00", PaymentStatus.PENDING));

        ReconciliationResult result = reconcile(record("PAY-CCC333CCC333", "250.00", "SUCCESSFUL"));

        assertThat(result.statusMismatches()).singleElement().satisfies(item -> {
            assertThat(item.ourStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(item.providerStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        });
    }

    @Test
    void reportsAmountMismatchBeforeStatusMismatch() {
        givenOurPayments(payment("PAY-CCC333CCC333", "250.00", PaymentStatus.PENDING));

        ReconciliationResult result = reconcile(record("PAY-CCC333CCC333", "300.00", "FAILED"));

        assertThat(result.amountMismatches()).hasSize(1);
        assertThat(result.statusMismatches()).isEmpty();
    }

    @Test
    void reportsRecordMissingOnOurSide() {
        givenOurPayments();

        ReconciliationResult result = reconcile(record("PAY-ZZZ999ZZZ999", "400.00", "SUCCESSFUL"));

        assertThat(result.missingOnOurSide()).singleElement().satisfies(item -> {
            assertThat(item.reference()).isEqualTo("PAY-ZZZ999ZZZ999");
            assertThat(item.ourAmount()).isNull();
            assertThat(item.providerAmount()).isEqualByComparingTo("400.00");
        });
    }

    @Test
    void reportsFinishedPaymentMissingOnProviderSide() {
        givenOurPayments(payment("PAY-AAA111AAA111", "1500.00", PaymentStatus.SUCCESSFUL));
        givenFinishedPayments(
                payment("PAY-AAA111AAA111", "1500.00", PaymentStatus.SUCCESSFUL),
                payment("PAY-DDD444DDD444", "700.00", PaymentStatus.SUCCESSFUL));

        ReconciliationResult result = reconcile(record("PAY-AAA111AAA111", "1500.00", "SUCCESSFUL"));

        assertThat(result.missingOnProviderSide()).singleElement().satisfies(item -> {
            assertThat(item.reference()).isEqualTo("PAY-DDD444DDD444");
            assertThat(item.providerAmount()).isNull();
        });
    }

    @Test
    void looksOnlyAtFinishedPaymentsForMissingOnProviderSide() {
        givenOurPayments();
        givenFinishedPayments();

        reconcile(record("PAY-ZZZ999ZZZ999", "400.00", "SUCCESSFUL"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<PaymentStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(paymentRepository).findAllByStatusIn(statuses.capture());
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(PaymentStatus.SUCCESSFUL, PaymentStatus.FAILED);
    }

    @Test
    void buildsSummaryTotals() {
        givenOurPayments(
                payment("PAY-AAA111AAA111", "1500.00", PaymentStatus.SUCCESSFUL),
                payment("PAY-BBB222BBB222", "1000.00", PaymentStatus.SUCCESSFUL),
                payment("PAY-CCC333CCC333", "250.00", PaymentStatus.PENDING));
        givenFinishedPayments(
                payment("PAY-AAA111AAA111", "1500.00", PaymentStatus.SUCCESSFUL),
                payment("PAY-BBB222BBB222", "1000.00", PaymentStatus.SUCCESSFUL),
                payment("PAY-DDD444DDD444", "700.00", PaymentStatus.SUCCESSFUL));

        ReconciliationResult result = reconcile(
                record("PAY-AAA111AAA111", "1500.00", "SUCCESSFUL"),
                record("PAY-BBB222BBB222", "900.00", "SUCCESSFUL"),
                record("PAY-CCC333CCC333", "250.00", "FAILED"),
                record("PAY-ZZZ999ZZZ999", "400.00", "SUCCESSFUL"));

        assertThat(result.summary().providerRecords()).isEqualTo(4);
        assertThat(result.summary().matched()).isEqualTo(1);
        assertThat(result.summary().amountMismatches()).isEqualTo(1);
        assertThat(result.summary().statusMismatches()).isEqualTo(1);
        assertThat(result.summary().missingOnOurSide()).isEqualTo(1);
        assertThat(result.summary().missingOnProviderSide()).isEqualTo(1);
        assertThat(result.summary().matchedAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void rejectsDuplicateReferences() {
        assertThatThrownBy(() -> reconcile(
                record("PAY-AAA111AAA111", "1500.00", "SUCCESSFUL"),
                record("PAY-AAA111AAA111", "1500.00", "SUCCESSFUL")))
                .isInstanceOf(DuplicateProviderRecordException.class)
                .hasMessage("Provider records contain duplicate references: PAY-AAA111AAA111");
        verifyNoInteractions(paymentRepository);
    }

    private ReconciliationResult reconcile(ProviderRecord... records) {
        return reconciliationService.reconcile(new ReconciliationRequest(List.of(records)));
    }

    private void givenOurPayments(Payment... payments) {
        when(paymentRepository.findAllByReferenceIn(any())).thenReturn(List.of(payments));
    }

    private void givenFinishedPayments(Payment... payments) {
        when(paymentRepository.findAllByStatusIn(any())).thenReturn(List.of(payments));
    }

    private ProviderRecord record(String reference, String amount, String status) {
        return new ProviderRecord(reference, new BigDecimal(amount), status);
    }

    private Payment payment(String reference, String amount, PaymentStatus status) {
        Payment payment = new Payment(reference, "INV-" + reference, new BigDecimal(amount), "KES", "254712345678");
        if (status != PaymentStatus.PENDING) {
            payment.complete(status, "QJK3H2L9P0");
        }
        return payment;
    }
}
