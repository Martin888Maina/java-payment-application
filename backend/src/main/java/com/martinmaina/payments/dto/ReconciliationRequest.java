package com.martinmaina.payments.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ReconciliationRequest(

        @NotEmpty
        @Size(max = 1000)
        List<@Valid ProviderRecord> records) {
}
