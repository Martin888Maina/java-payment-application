package com.martinmaina.payments.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.martinmaina.payments.config.CallbackProperties;
import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.dto.PageResponse;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.DuplicatePaymentException;
import com.martinmaina.payments.exception.PaymentNotFoundException;
import com.martinmaina.payments.service.PaymentService;
import com.martinmaina.payments.service.SignatureVerifier;

@WebMvcTest(PaymentController.class)
@EnableConfigurationProperties(CallbackProperties.class)
@Import(SignatureVerifier.class)
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

    @Test
    void returnsPaymentByReference() throws Exception {
        when(paymentService.findByReference("PAY-7F3K9Q2M8XWD")).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/v1/payments/PAY-7F3K9Q2M8XWD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("PAY-7F3K9Q2M8XWD"))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returns404ForUnknownPayment() throws Exception {
        when(paymentService.findByReference("PAY-UNKNOWN")).thenThrow(new PaymentNotFoundException("PAY-UNKNOWN"));

        mockMvc.perform(get("/api/v1/payments/PAY-UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Payment PAY-UNKNOWN was not found"));
    }

    @Test
    void listsFirstPageOfTenByDefault() throws Exception {
        when(paymentService.list(null, 0, 10)).thenReturn(onePage(0, 10));

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reference").value("PAY-7F3K9Q2M8XWD"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void passesStatusPageAndSizeToService() throws Exception {
        when(paymentService.list(PaymentStatus.PENDING, 2, 5)).thenReturn(onePage(2, 5));

        mockMvc.perform(get("/api/v1/payments").param("status", "PENDING").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
        verify(paymentService).list(PaymentStatus.PENDING, 2, 5);
    }

    @Test
    void returns400ForNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than or equal to 0"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForPageOverOneMillion() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("page", "1000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.errors[0].message").value("must be less than or equal to 1000000"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForPageSizeOfZero() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than or equal to 1"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForPageSizeOverOneHundred() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].message").value("must be less than or equal to 100"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(paymentService);
    }

    private ResultActions postPayment(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private PageResponse<PaymentResponse> onePage(int page, int size) {
        return new PageResponse<>(List.of(paymentResponse()), page, size, 21, 3);
    }

    private PaymentResponse paymentResponse() {
        Instant now = Instant.parse("2026-10-03T12:00:00Z");
        return new PaymentResponse("PAY-7F3K9Q2M8XWD", "INV-1001", new BigDecimal("1500.00"), "KES",
                "254712345678", PaymentStatus.PENDING, null, now, now);
    }
}
