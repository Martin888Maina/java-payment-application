package com.martinmaina.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("payments.callback")
public record CallbackProperties(@NotBlank String secret) {
}
