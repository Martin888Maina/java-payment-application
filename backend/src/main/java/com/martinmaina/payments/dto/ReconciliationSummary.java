package com.martinmaina.payments.dto;

import java.math.BigDecimal;

public record ReconciliationSummary(
        int providerRecords,
        int matched,
        int amountMismatches,
        int statusMismatches,
        int missingOnOurSide,
        int missingOnProviderSide,
        BigDecimal matchedAmount) {
}
