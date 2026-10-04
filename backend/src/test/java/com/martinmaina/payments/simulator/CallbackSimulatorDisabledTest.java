package com.martinmaina.payments.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CallbackSimulatorDisabledTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Test
    void simulatorDoesNotExistOutsideDevProfile() {
        assertThat(context.getBeanNamesForType(CallbackSimulatorController.class)).isEmpty();

        int status = RestClient.create().post()
                .uri("http://localhost:" + port + "/api/v1/dev/payments/PAY-ANY/simulate-callback")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"SUCCESSFUL\"}")
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }

    @Test
    void demoDataResetDoesNotExistOutsideDevProfile() {
        assertThat(context.getBeanNamesForType(DemoDataController.class)).isEmpty();

        int status = RestClient.create().delete()
                .uri("http://localhost:" + port + "/api/v1/dev/payments")
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }
}
