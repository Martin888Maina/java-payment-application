package com.martinmaina.payments.dto;

import java.util.List;

public record ReconciliationResult(
        ReconciliationSummary summary,
        List<ReconciliationItem> matched,
        List<ReconciliationItem> amountMismatches,
        List<ReconciliationItem> statusMismatches,
        List<ReconciliationItem> missingOnOurSide,
        List<ReconciliationItem> missingOnProviderSide) {
}
