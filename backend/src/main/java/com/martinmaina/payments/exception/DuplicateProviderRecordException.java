package com.martinmaina.payments.exception;

import java.util.List;

public class DuplicateProviderRecordException extends RuntimeException {

    public DuplicateProviderRecordException(List<String> references) {
        super("Provider records contain duplicate references: " + String.join(", ", references));
    }
}
