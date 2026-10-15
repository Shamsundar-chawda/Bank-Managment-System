package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountStatementServiceTest {

    private final AccountStatementService statementService = new AccountStatementService();

    @Test
    void generatesStatementWithAccountDetailsAndDebitsAndCredits() {
        Account account = new Account();
        account.setId(1234567890L);
        account.setUsername("alex");
        account.setFullName("Alex Doe");
        account.setBalance(new BigDecimal("125.50"));

        Transaction deposit = new Transaction(
                new BigDecimal("100.00"), "Deposit", LocalDateTime.of(2026, 10, 1, 9, 30), account);
        deposit.setId(1L);
        Transaction transfer = new Transaction(
                new BigDecimal("25.50"), "Transfer to sam", LocalDateTime.of(2026, 10, 2, 10, 0), account);
        transfer.setId(2L);

        String csv = new String(statementService.generateCsv(
                account, List.of(deposit, transfer), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
                StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"Account holder\",\"Alex Doe\""));
        assertTrue(csv.contains("\"Account number\",\"1234567890\""));
        assertTrue(csv.contains("\"Current balance\",\"125.50\""));
        assertTrue(csv.contains("\"1\",\"2026-10-01 09:30:00\",\"Deposit\",\"\",\"100.00\""));
        assertTrue(csv.contains("\"2\",\"2026-10-02 10:00:00\",\"Transfer to sam\",\"25.50\",\"\""));
    }

    @Test
    void escapesQuotesAndGuardsSpreadsheetFormulaText() {
        Account account = new Account();
        account.setId(1234567890L);
        account.setUsername("  =HYPERLINK(\"https://example.com\")");
        account.setBalance(BigDecimal.ZERO);

        Transaction transaction = new Transaction(
                BigDecimal.ONE, "Transfer to \"=SUM(A1:A2)", LocalDateTime.of(2026, 10, 1, 9, 30), account);
        transaction.setId(1L);

        String csv = new String(statementService.generateCsv(account, List.of(transaction), null, null),
                StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Username\",\"'  =HYPERLINK(\"\"https://example.com\"\")\""));
        assertTrue(csv.contains("\"Transfer to \"\"=SUM(A1:A2)\""));
    }
}
