package com.martinmaina.payments.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.repository.PaymentRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DemoDataControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void deletesAllPayments() {
        paymentRepository.save(newPayment("PAY-DEMODATA0001", "INV-9001"));
        paymentRepository.save(newPayment("PAY-DEMODATA0002", "INV-9002"));

        int status = RestClient.create().delete()
                .uri("http://localhost:" + port + "/api/v1/dev/payments")
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isEqualTo(204);
        assertThat(paymentRepository.count()).isZero();
    }

    private Payment newPayment(String reference, String merchantReference) {
        return new Payment(reference, merchantReference, new BigDecimal("1500.00"), "KES", "254712345678");
    }
}
