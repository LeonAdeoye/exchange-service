package com.leon.validation;

import org.springframework.stereotype.Component;

@Component
public class OrderMessageValidator {
    public ValidationResult validateMessage(String data)
    {
        return new ValidationResult(true, "");
    }
}