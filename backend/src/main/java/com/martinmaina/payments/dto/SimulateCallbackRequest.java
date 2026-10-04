package com.martinmaina.payments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SimulateCallbackRequest(

        @NotNull
        @Pattern(regexp = "SUCCESSFUL|FAILED", message = "must be SUCCESSFUL or FAILED")
        String status) {
}
