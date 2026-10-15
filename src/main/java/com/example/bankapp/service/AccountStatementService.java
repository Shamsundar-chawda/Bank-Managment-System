package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AccountStatementService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateCsv(Account account, List<Transaction> transactions, LocalDate fromDate, LocalDate toDate) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendRow(csv, "BankEase Account Statement");
        appendRow(csv, "Account holder", account.getFullName() == null || account.getFullName().isBlank()
                ? account.getUsername() : account.getFullName());
        appendRow(csv, "Username", account.getUsername());
        appendRow(csv, "Account number", account.getId().toString());
        appendRow(csv, "Statement from", fromDate == null ? "All available history" : fromDate.toString());
        appendRow(csv, "Statement to", toDate == null ? "Present" : toDate.toString());
        appendRow(csv, "Current balance", amount(account.getBalance()));
        csv.append('\n');
        appendRow(csv, "Transaction ID", "Date and time", "Description", "Debit", "Credit");

        for (Transaction transaction : transactions) {
            boolean credit = isCredit(transaction.getType());
            appendRow(csv,
                    transaction.getId().toString(),
                    TIMESTAMP_FORMAT.format(transaction.getTimestamp()),
                    transaction.getType(),
                    credit ? "" : amount(transaction.getAmount()),
                    credit ? amount(transaction.getAmount()) : "");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private boolean isCredit(String type) {
        return "Deposit".equals(type) || (type != null && type.startsWith("Receive from "));
    }

    private String amount(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvField(values[i]));
        }
        csv.append("\r\n");
    }

    private String csvField(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safeValue = value;
        String trimmedValue = safeValue.stripLeading();
        if ((!trimmedValue.isEmpty() && "=+-@".indexOf(trimmedValue.charAt(0)) >= 0)
                || (!safeValue.isEmpty() && "\t\r\n".indexOf(safeValue.charAt(0)) >= 0)) {
            safeValue = "'" + safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
