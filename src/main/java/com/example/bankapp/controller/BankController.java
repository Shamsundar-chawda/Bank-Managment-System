package com.example.bankapp.controller;


import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import com.example.bankapp.service.AccountStatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Controller
public class BankController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountStatementService accountStatementService;

    @GetMapping("/")
    public String index() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin/dashboard" : "redirect:/dashboard";
    }

    @GetMapping("/admin")
    public String adminLoginPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        return "admin_login";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("customerAccounts", accountService.getCustomerAccounts());
        return "admin_dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Account account = getCurrentAccount();

        model.addAttribute("account", account);
        return "dashboard";
    }

    
    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerAccount(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            accountService.registerAccount(username, password);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";

        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount, Model model) {
        Account account = getCurrentAccount();

        try {
            accountService.deposit(account, amount);

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam BigDecimal amount, Model model) {
        Account account = getCurrentAccount();

        try {
            accountService.withdraw(account, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transactionHistory(Model model) {
        Account account = getCurrentAccount();

        model.addAttribute("transactions", accountService.getTransactionHistory(account));
        return "transactions";
    }

    @GetMapping("/transactions/statement")
    public ResponseEntity<byte[]> downloadAccountStatement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "Start date must not be after end date");
        }

        Account account = getCurrentAccount();
        List<Transaction> transactions = accountService.getTransactionHistory(account, fromDate, toDate);
        byte[] statement = accountStatementService.generateCsv(account, transactions, fromDate, toDate);
        String filename = "account-statement-" + account.getId() + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(statement);
    }

    @PostMapping("/transfer")
    public String transferAmount(@RequestParam String toUsername, @RequestParam BigDecimal amount, Model model) {
        Account fromAccount = getCurrentAccount();

        try {
            accountService.transferAmount(fromAccount, toUsername, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", fromAccount);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model,
                          @RequestParam(required = false) String success,
                          @RequestParam(required = false) String error) {
        Account account = getCurrentAccount();

        model.addAttribute("account", account);
        if (success != null) {
            model.addAttribute("success", success);
        }
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address) {
        Account account = getCurrentAccount();

        try {
            accountService.updateProfile(account, fullName, email, phone, address);
            return "redirect:/profile?success=Profile+updated+successfully!";
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + e.getMessage();
        }
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {
        Account account = getCurrentAccount();

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/profile?error=New+passwords+do+not+match!";
        }

        try {
            accountService.changePassword(account, currentPassword, newPassword);
            return "redirect:/profile?success=Password+changed+successfully!";
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + e.getMessage();
        }
    }

    private Account getCurrentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountService.findAccountByUsername(username);
    }
}
