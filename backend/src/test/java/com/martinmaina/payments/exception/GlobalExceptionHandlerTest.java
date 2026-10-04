package com.martinmaina.payments.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.martinmaina.payments.controller.PaymentController;
import com.martinmaina.payments.controller.ReconciliationController;
import com.martinmaina.payments.service.PaymentService;
import com.martinmaina.payments.service.ReconciliationService;
import com.martinmaina.payments.service.SignatureVerifier;

@WebMvcTest({PaymentController.class, ReconciliationController.class})
@EnableConfigurationProperties(CallbackProperties.class)
@Import(SignatureVerifier.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Test
    void returns400ForMalformedJson() throws Exception {
        postJson("/api/v1/payments", "{\"amount\": 15")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request body is missing or is not valid JSON"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void returns400ForMissingBody() throws Exception {
        postJson("/api/v1/payments", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void namesTheFieldWithTheWrongType() throws Exception {
        postJson("/api/v1/payments", """
                {"merchantReference": "INV-1001", "amount": "abc", "payerPhone": "254712345678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Field 'amount' has a value of the wrong type"));
    }

    @Test
    void namesThePositionOfAWrongTypeInsideAList() throws Exception {
        postJson("/api/v1/reconciliation", """
                {"records": [
                    {"reference": "PAY-AAA111AAA111", "amount": 1500.00, "status": "SUCCESSFUL"},
                    {"reference": "PAY-BBB222BBB222", "amount": "x", "status": "SUCCESSFUL"}
                ]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Field 'records[1].amount' has a value of the wrong type"));
        verifyNoInteractions(reconciliationService);
    }

    @Test
    void listsAllowedValuesForAnInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Parameter 'status' must be one of: PENDING, SUCCESSFUL, FAILED"));
    }

    @Test
    void returns404WithPlainMessageForUnknownRoute() throws Exception {
        mockMvc.perform(get("/api/v1/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No endpoint matches GET /api/v1/nothing-here"));
    }

    @Test
    void returns405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(delete("/api/v1/payments"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void hidesDetailsOfUnexpectedErrors() throws Exception {
        when(paymentService.findByReference("PAY-7F3K9Q2M8XWD"))
                .thenThrow(new IllegalStateException("connection to db-internal-01 refused"));

        mockMvc.perform(get("/api/v1/payments/PAY-7F3K9Q2M8XWD"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("db-internal-01"))));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
