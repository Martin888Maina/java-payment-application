package com.martinmaina.payments.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.martinmaina.payments.config.CallbackProperties;
import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.PaymentAlreadyFinalisedException;
import com.martinmaina.payments.exception.PaymentNotFoundException;
import com.martinmaina.payments.service.CallbackService;
import com.martinmaina.payments.service.SignatureVerifier;

@WebMvcTest(CallbackController.class)
@EnableConfigurationProperties(CallbackProperties.class)
@Import(SignatureVerifier.class)
class CallbackControllerTest {

    private static final String URL = "/api/v1/payments/callback";
    private static final String SUCCESS_BODY = """
            {"reference":"PAY-7F3K9Q2M8XWD","status":"SUCCESSFUL","providerReference":"QJK3H2L9P0"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignatureVerifier signatureVerifier;

    @MockitoBean
    private CallbackService callbackService;

    @Test
    void appliesValidCallback() throws Exception {
        when(callbackService.apply(any(CallbackRequest.class))).thenReturn(paymentResponse(PaymentStatus.SUCCESSFUL));

        postSigned(SUCCESS_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.providerReference").value("QJK3H2L9P0"));

        ArgumentCaptor<CallbackRequest> captor = ArgumentCaptor.forClass(CallbackRequest.class);
        verify(callbackService).apply(captor.capture());
        assertThat(captor.getValue().reference()).isEqualTo("PAY-7F3K9Q2M8XWD");
        assertThat(captor.getValue().result()).isEqualTo(PaymentStatus.SUCCESSFUL);
    }

    @Test
    void returns401ForWrongSignature() throws Exception {
        sendCallback(SUCCESS_BODY, "0000000000000000000000000000000000000000000000000000000000000000")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Callback signature is missing or invalid"));
        verifyNoInteractions(callbackService);
    }

    @Test
    void returns401WhenSignatureIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUCCESS_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(callbackService);
    }

    @Test
    void returns401WhenBodyWasChangedAfterSigning() throws Exception {
        String tampered = SUCCESS_BODY.replace("SUCCESSFUL", "FAILED");

        sendCallback(tampered, signatureVerifier.sign(SUCCESS_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(callbackService);
    }

    @Test
    void returns400ForPendingStatus() throws Exception {
        postSigned("""
                {"reference":"PAY-7F3K9Q2M8XWD","status":"PENDING"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value("must be SUCCESSFUL or FAILED"));
        verifyNoInteractions(callbackService);
    }

    @Test
    void returns400WhenReferenceIsMissing() throws Exception {
        postSigned("""
                {"status":"SUCCESSFUL"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reference"));
        verifyNoInteractions(callbackService);
    }

    @Test
    void returns404ForUnknownPayment() throws Exception {
        when(callbackService.apply(any(CallbackRequest.class)))
                .thenThrow(new PaymentNotFoundException("PAY-7F3K9Q2M8XWD"));

        postSigned(SUCCESS_BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Payment PAY-7F3K9Q2M8XWD was not found"));
    }

    @Test
    void returns409WhenPaymentIsAlreadyFinished() throws Exception {
        when(callbackService.apply(any(CallbackRequest.class)))
                .thenThrow(new PaymentAlreadyFinalisedException("PAY-7F3K9Q2M8XWD", PaymentStatus.FAILED));

        postSigned(SUCCESS_BODY)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Payment PAY-7F3K9Q2M8XWD is already FAILED and cannot change"));
    }

    private ResultActions postSigned(String body) throws Exception {
        return sendCallback(body, signatureVerifier.sign(body));
    }

    private ResultActions sendCallback(String body, String signature) throws Exception {
        return mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(CallbackSignatureAdvice.SIGNATURE_HEADER, signature)
                .content(body));
    }

    private PaymentResponse paymentResponse(PaymentStatus status) {
        Instant now = Instant.parse("2026-10-03T12:00:00Z");
        return new PaymentResponse("PAY-7F3K9Q2M8XWD", "INV-1001", new BigDecimal("1500.00"), "KES",
                "254712345678", status, "QJK3H2L9P0", now, now);
    }
}
