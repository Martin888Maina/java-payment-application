package com.martinmaina.payments.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(

        @NotBlank
        @Size(max = 64)
        String merchantReference,

        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @Pattern(regexp = "[A-Z]{3}", message = "must be a three letter currency code")
        String currency,

        @NotBlank
        @Pattern(regexp = "254[17]\\d{8}", message = "must be in the format 2547XXXXXXXX or 2541XXXXXXXX")
        String payerPhone) {
}
