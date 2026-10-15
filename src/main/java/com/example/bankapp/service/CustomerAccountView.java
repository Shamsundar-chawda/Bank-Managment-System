package com.example.bankapp.service;

import java.math.BigDecimal;

public record CustomerAccountView(
        Long id,
        String username,
        String fullName,
        String email,
        BigDecimal balance) {
}
