package com.martinmaina.payments.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.martinmaina.payments.dto.ProviderRecord;
import com.martinmaina.payments.dto.ReconciliationItem;
import com.martinmaina.payments.dto.ReconciliationRequest;
import com.martinmaina.payments.dto.ReconciliationResult;
import com.martinmaina.payments.dto.ReconciliationSummary;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.repository.PaymentRepository;

@Service
public class ReconciliationService {

    private final PaymentRepository paymentRepository;

    public ReconciliationService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationResult reconcile(ReconciliationRequest request) {
        List<ProviderRecord> records = request.records();
        Map<String, Payment> ourPayments = paymentRepository
                .findAllByReferenceIn(records.stream().map(ProviderRecord::reference).toList())
                .stream()
                .collect(Collectors.toMap(Payment::getReference, Function.identity()));

        List<ReconciliationItem> matched = new ArrayList<>();
        List<ReconciliationItem> amountMismatches = new ArrayList<>();
        List<ReconciliationItem> missingOnOurSide = new ArrayList<>();

        for (ProviderRecord record : records) {
            Payment payment = ourPayments.get(record.reference());
            if (payment == null) {
                missingOnOurSide.add(ReconciliationItem.providerOnly(record));
            } else if (payment.getAmount().compareTo(record.amount()) != 0) {
                amountMismatches.add(ReconciliationItem.of(payment, record));
            } else {
                matched.add(ReconciliationItem.of(payment, record));
            }
        }

        BigDecimal matchedAmount = matched.stream()
                .map(ReconciliationItem::providerAmount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);

        ReconciliationSummary summary = new ReconciliationSummary(records.size(), matched.size(),
                amountMismatches.size(), 0, missingOnOurSide.size(), 0, matchedAmount);
        return new ReconciliationResult(summary, matched, amountMismatches, List.of(), missingOnOurSide, List.of());
    }
}
