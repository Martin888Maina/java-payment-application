package com.martinmaina.payments.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.repository.PaymentRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class CallbackSimulatorControllerTest {

    private static final String REFERENCE = "PAY-SIMULATED001";

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository paymentRepository;

    private final RestClient restClient = RestClient.create();

    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void simulatedCallbackCompletesPayment() {
        savePendingPayment();

        Reply reply = simulate(REFERENCE, "SUCCESSFUL");

        assertThat(reply.status()).isEqualTo(200);
        Payment stored = paymentRepository.findByReference(REFERENCE).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        assertThat(stored.getProviderReference()).startsWith("SIM");
    }

    @Test
    void passesOnTheCallbackErrorForFinishedPayment() {
        savePendingPayment();
        simulate(REFERENCE, "SUCCESSFUL");

        Reply reply = simulate(REFERENCE, "FAILED");

        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.body()).contains("is already SUCCESSFUL and cannot change");
        assertThat(paymentRepository.findByReference(REFERENCE).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESSFUL);
    }

    private void savePendingPayment() {
        paymentRepository.save(new Payment(REFERENCE, "INV-6001", new BigDecimal("1500.00"), "KES", "254712345678"));
    }

    private Reply simulate(String reference, String status) {
        return restClient.post()
                .uri("http://localhost:" + port + "/api/v1/dev/payments/" + reference + "/simulate-callback")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"" + status + "\"}")
                .exchange((request, response) -> new Reply(
                        response.getStatusCode().value(),
                        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private record Reply(int status, String body) {
    }
}
