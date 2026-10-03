package com.martinmaina.payments.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.DuplicatePaymentException;
import com.martinmaina.payments.service.PaymentService;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    private static final String VALID_BODY = """
            {"merchantReference": "INV-1001", "amount": 1500.00, "payerPhone": "254712345678"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void returns201WithLocationForNewPayment() throws Exception {
        when(paymentService.create(any(CreatePaymentRequest.class)))
                .thenReturn(new PaymentService.CreateResult(paymentResponse(), true));

        postPayment(VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/payments/PAY-7F3K9Q2M8XWD")))
                .andExpect(jsonPath("$.reference").value("PAY-7F3K9Q2M8XWD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("KES"));
    }

    @Test
    void returns200ForRepeatedRequest() throws Exception {
        when(paymentService.create(any(CreatePaymentRequest.class)))
                .thenReturn(new PaymentService.CreateResult(paymentResponse(), false));

        postPayment(VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.reference").value("PAY-7F3K9Q2M8XWD"));
    }

    @Test
    void returns400WhenAmountIsMissing() throws Exception {
        postPayment("""
                {"merchantReference": "INV-1001", "payerPhone": "254712345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400WhenAmountIsZero() throws Exception {
        postPayment("""
                {"merchantReference": "INV-1001", "amount": 0, "payerPhone": "254712345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than 0"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400WhenAmountHasMoreThanTwoDecimals() throws Exception {
        postPayment("""
                {"merchantReference": "INV-1001", "amount": 10.005, "payerPhone": "254712345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForInvalidPhoneNumber() throws Exception {
        postPayment("""
                {"merchantReference": "INV-1001", "amount": 1500.00, "payerPhone": "0712345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("payerPhone"))
                .andExpect(jsonPath("$.errors[0].message").value("must be in the format 2547XXXXXXXX or 2541XXXXXXXX"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForInvalidCurrency() throws Exception {
        postPayment("""
                {"merchantReference": "INV-1001", "amount": 1500.00, "currency": "kes", "payerPhone": "254712345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("currency"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a three letter currency code"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns409ForConflictingRequest() throws Exception {
        when(paymentService.create(any(CreatePaymentRequest.class)))
                .thenThrow(new DuplicatePaymentException("INV-1001"));

        postPayment(VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("Merchant reference INV-1001 is already used by a payment with different details"));
    }

    private ResultActions postPayment(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private PaymentResponse paymentResponse() {
        Instant now = Instant.parse("2026-10-03T12:00:00Z");
        return new PaymentResponse("PAY-7F3K9Q2M8XWD", "INV-1001", new BigDecimal("1500.00"), "KES",
                "254712345678", PaymentStatus.PENDING, null, now, now);
    }
}
