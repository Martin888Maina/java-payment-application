package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.repository.PaymentRepository;

@SpringBootTest
class CallbackServiceIntegrationTest {

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void savesCallbackResultWithNewUpdatedTime() {
        paymentRepository.save(
                new Payment("PAY-CALLBACK0001", "INV-4001", new BigDecimal("1500.00"), "KES", "254712345678"));
        Instant anHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        jdbcTemplate.update("update payments set created_at = ?, updated_at = ?",
                Timestamp.from(anHourAgo), Timestamp.from(anHourAgo));

        PaymentResponse response = callbackService.apply(
                new CallbackRequest("PAY-CALLBACK0001", "SUCCESSFUL", "QJK3H2L9P0"));

        Payment stored = paymentRepository.findByReference("PAY-CALLBACK0001").orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        assertThat(stored.getProviderReference()).isEqualTo("QJK3H2L9P0");
        assertThat(stored.getCreatedAt()).isEqualTo(anHourAgo);
        assertThat(stored.getUpdatedAt()).isAfter(anHourAgo);
        assertThat(response.updatedAt()).isEqualTo(stored.getUpdatedAt());
    }
}
