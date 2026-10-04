package com.martinmaina.payments.dto;

import java.math.BigDecimal;

import com.martinmaina.payments.entity.PaymentStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProviderRecord(

        @NotBlank
        @Size(max = 20)
        String reference,

        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotNull
        @Pattern(regexp = "SUCCESSFUL|FAILED", message = "must be SUCCESSFUL or FAILED")
        String status) {

    public PaymentStatus result() {
        return PaymentStatus.valueOf(status);
    }
}
