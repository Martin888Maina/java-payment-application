package com.martinmaina.payments.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.martinmaina.payments.dto.ReconciliationRequest;
import com.martinmaina.payments.dto.ReconciliationResult;
import com.martinmaina.payments.service.ReconciliationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    public ReconciliationResult reconcile(@Valid @RequestBody ReconciliationRequest request) {
        return reconciliationService.reconcile(request);
    }
}
