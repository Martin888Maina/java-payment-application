package com.martinmaina.payments.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.DuplicateProviderRecordException;
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
        List<String> duplicates = duplicateReferences(records);
        if (!duplicates.isEmpty()) {
            throw new DuplicateProviderRecordException(duplicates);
        }

        Set<String> providerReferences = records.stream().map(ProviderRecord::reference).collect(Collectors.toSet());
        Map<String, Payment> ourPayments = paymentRepository.findAllByReferenceIn(providerReferences)
                .stream()
                .collect(Collectors.toMap(Payment::getReference, Function.identity()));

        List<ReconciliationItem> matched = new ArrayList<>();
        List<ReconciliationItem> amountMismatches = new ArrayList<>();
        List<ReconciliationItem> statusMismatches = new ArrayList<>();
        List<ReconciliationItem> missingOnOurSide = new ArrayList<>();

        for (ProviderRecord record : records) {
            Payment payment = ourPayments.get(record.reference());
            if (payment == null) {
                missingOnOurSide.add(ReconciliationItem.providerOnly(record));
            } else if (payment.getAmount().compareTo(record.amount()) != 0) {
                amountMismatches.add(ReconciliationItem.of(payment, record));
            } else if (payment.getStatus() != record.result()) {
                statusMismatches.add(ReconciliationItem.of(payment, record));
            } else {
                matched.add(ReconciliationItem.of(payment, record));
            }
        }

        // Pending payments are still in progress, so the provider may not list them yet
        List<ReconciliationItem> missingOnProviderSide = paymentRepository
                .findAllByStatusIn(List.of(PaymentStatus.SUCCESSFUL, PaymentStatus.FAILED))
                .stream()
                .filter(payment -> !providerReferences.contains(payment.getReference()))
                .map(ReconciliationItem::ourOnly)
                .sorted(Comparator.comparing(ReconciliationItem::reference))
                .toList();

        BigDecimal matchedAmount = matched.stream()
                .map(ReconciliationItem::providerAmount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);

        ReconciliationSummary summary = new ReconciliationSummary(records.size(), matched.size(),
                amountMismatches.size(), statusMismatches.size(), missingOnOurSide.size(),
                missingOnProviderSide.size(), matchedAmount);
        return new ReconciliationResult(summary, matched, amountMismatches, statusMismatches, missingOnOurSide,
                missingOnProviderSide);
    }

    private List<String> duplicateReferences(List<ProviderRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(ProviderRecord::reference, TreeMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
    }
}
