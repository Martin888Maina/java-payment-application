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
import java.util.List;

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
import com.martinmaina.payments.dto.ReconciliationItem;
import com.martinmaina.payments.dto.ReconciliationRequest;
import com.martinmaina.payments.dto.ReconciliationResult;
import com.martinmaina.payments.dto.ReconciliationSummary;
import com.martinmaina.payments.entity.PaymentStatus;
import com.martinmaina.payments.exception.DuplicateProviderRecordException;
import com.martinmaina.payments.service.ReconciliationService;
import com.martinmaina.payments.service.SignatureVerifier;

@WebMvcTest(ReconciliationController.class)
@EnableConfigurationProperties(CallbackProperties.class)
@Import(SignatureVerifier.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Test
    void returnsReconciliationResult() throws Exception {
        when(reconciliationService.reconcile(any(ReconciliationRequest.class))).thenReturn(result());

        postRecords("""
                {"records": [{"reference": "PAY-AAA111AAA111", "amount": 1500.00, "status": "SUCCESSFUL"}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.providerRecords").value(1))
                .andExpect(jsonPath("$.summary.matched").value(1))
                .andExpect(jsonPath("$.summary.matchedAmount").value(1500.00))
                .andExpect(jsonPath("$.matched[0].reference").value("PAY-AAA111AAA111"))
                .andExpect(jsonPath("$.missingOnOurSide").isEmpty());

        ArgumentCaptor<ReconciliationRequest> captor = ArgumentCaptor.forClass(ReconciliationRequest.class);
        verify(reconciliationService).reconcile(captor.capture());
        assertThat(captor.getValue().records()).singleElement().satisfies(record -> {
            assertThat(record.reference()).isEqualTo("PAY-AAA111AAA111");
            assertThat(record.amount()).isEqualByComparingTo("1500.00");
            assertThat(record.result()).isEqualTo(PaymentStatus.SUCCESSFUL);
        });
    }

    @Test
    void returns400ForEmptyRecords() throws Exception {
        postRecords("""
                {"records": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("records"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be empty"));
        verifyNoInteractions(reconciliationService);
    }

    @Test
    void returns400NamingTheRecordWithABadStatus() throws Exception {
        postRecords("""
                {"records": [
                    {"reference": "PAY-AAA111AAA111", "amount": 1500.00, "status": "SUCCESSFUL"},
                    {"reference": "PAY-BBB222BBB222", "amount": 900.00, "status": "PENDING"}
                ]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("records[1].status"))
                .andExpect(jsonPath("$.errors[0].message").value("must be SUCCESSFUL or FAILED"));
        verifyNoInteractions(reconciliationService);
    }

    @Test
    void returns400ForNegativeAmount() throws Exception {
        postRecords("""
                {"records": [{"reference": "PAY-AAA111AAA111", "amount": -5, "status": "SUCCESSFUL"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("records[0].amount"));
        verifyNoInteractions(reconciliationService);
    }

    @Test
    void returns400ForDuplicateReferences() throws Exception {
        when(reconciliationService.reconcile(any(ReconciliationRequest.class)))
                .thenThrow(new DuplicateProviderRecordException(List.of("PAY-AAA111AAA111")));

        postRecords("""
                {"records": [
                    {"reference": "PAY-AAA111AAA111", "amount": 1500.00, "status": "SUCCESSFUL"},
                    {"reference": "PAY-AAA111AAA111", "amount": 1500.00, "status": "SUCCESSFUL"}
                ]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Provider records contain duplicate references: PAY-AAA111AAA111"));
    }

    private ResultActions postRecords(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/reconciliation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ReconciliationResult result() {
        ReconciliationItem item = new ReconciliationItem("PAY-AAA111AAA111", new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), PaymentStatus.SUCCESSFUL, PaymentStatus.SUCCESSFUL);
        ReconciliationSummary summary = new ReconciliationSummary(1, 1, 0, 0, 0, 0, new BigDecimal("1500.00"));
        return new ReconciliationResult(summary, List.of(item), List.of(), List.of(), List.of(), List.of());
    }
}
