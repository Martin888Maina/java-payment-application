package com.martinmaina.payments.simulator;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.martinmaina.payments.controller.CallbackSignatureAdvice;
import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.SimulateCallbackRequest;
import com.martinmaina.payments.service.SignatureVerifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import tools.jackson.databind.json.JsonMapper;

// Plays the role of the payment provider during local development
@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev/payments")
public class CallbackSimulatorController {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SignatureVerifier signatureVerifier;
    private final JsonMapper jsonMapper;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom random = new SecureRandom();

    public CallbackSimulatorController(SignatureVerifier signatureVerifier, JsonMapper jsonMapper) {
        this.signatureVerifier = signatureVerifier;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping("/{reference}/simulate-callback")
    public ResponseEntity<String> simulate(@PathVariable String reference,
            @Valid @RequestBody SimulateCallbackRequest request, HttpServletRequest servletRequest) {

        CallbackRequest callback = new CallbackRequest(reference, request.status(), providerReference());
        String body = jsonMapper.writeValueAsString(callback);

        // Sent to this server only, never to the host named in the request
        String callbackUrl = "http://localhost:" + servletRequest.getLocalPort()
                + servletRequest.getContextPath() + "/api/v1/payments/callback";

        return restClient.post()
                .uri(callbackUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header(CallbackSignatureAdvice.SIGNATURE_HEADER, signatureVerifier.sign(body))
                .body(body)
                .exchange((callbackRequest, response) -> ResponseEntity.status(response.getStatusCode())
                        .headers(headers -> headers.setContentType(response.getHeaders().getContentType()))
                        .body(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private String providerReference() {
        StringBuilder reference = new StringBuilder("SIM");
        for (int i = 0; i < 7; i++) {
            reference.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return reference.toString();
    }
}
