package com.martinmaina.payments.dto;

import com.martinmaina.payments.entity.PaymentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CallbackRequest(

        @NotBlank
        @Size(max = 20)
        String reference,

        @NotNull
        @Pattern(regexp = "SUCCESSFUL|FAILED", message = "must be SUCCESSFUL or FAILED")
        String status,

        @Size(max = 64)
        String providerReference) {

    public PaymentStatus result() {
        return PaymentStatus.valueOf(status);
    }
}
