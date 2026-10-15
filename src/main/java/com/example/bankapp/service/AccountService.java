package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${bank.admin.username:}")
    private String adminUsername;

    @Value("${bank.admin.password:}")
    private String adminPassword;

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public void registerAccount(String username, String password) {
        if (isAdminConfigured() && adminUsername.equals(username)) {
            throw new RuntimeException("This username is reserved");
        }
        if(accountRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Account already exists");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);

        accountRepository.save(account);
        
    }

    public void deposit(Account account, BigDecimal amount) {
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);

        throw new RuntimeException("Successfully deposited");
    }

    public void withdraw(Account account, BigDecimal amount) {
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        } else {
            if(account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountIdOrderByTimestampAsc(account.getId());
    }

    public List<Transaction> getTransactionHistory(Account account, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        if (start != null && end != null) {
            return transactionRepository.findByAccountIdAndTimestampBetweenOrderByTimestampAsc(
                    account.getId(), start, end);
        }
        if (start != null) {
            return transactionRepository.findByAccountIdAndTimestampGreaterThanEqualOrderByTimestampAsc(
                    account.getId(), start);
        }
        if (end != null) {
            return transactionRepository.findByAccountIdAndTimestampLessThanEqualOrderByTimestampAsc(
                    account.getId(), end);
        }
        return getTransactionHistory(account);
    }

    public List<CustomerAccountView> getCustomerAccounts() {
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "username")).stream()
                .map(account -> new CustomerAccountView(
                        account.getId(),
                        account.getUsername(),
                        account.getFullName(),
                        account.getEmail(),
                        account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (isAdminConfigured() && adminUsername.equals(username)) {
            return org.springframework.security.core.userdetails.User.withUsername(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles("ADMIN")
                    .build();
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        Account userDetails = new Account(
                account.getUsername(),
                account.getPassword(),
                account.getBalance(),
                account.getTransactions(),
                authorities()
        );
        userDetails.setId(account.getId());
        return userDetails;
    }

    public Collection<? extends GrantedAuthority> authorities(){
        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private boolean isAdminConfigured() {
        return adminUsername != null && !adminUsername.isBlank()
                && adminPassword != null && !adminPassword.isBlank();
    }

    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        } else {
            if(fromAccount.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
        }

        Account toAccount = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));
        // Deduct from main account
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // Transfer to recipient account
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        // Create transaction records
        Transaction debitTransaction = new Transaction(
                amount,
                "Transfer to " + toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        );
        transactionRepository.save(debitTransaction);

        // Receive Transaction
        Transaction receiveTransaction = new Transaction(
                amount,
                "Receive from " + fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        );
        transactionRepository.save(receiveTransaction);
    }

    public void updateProfile(Account account, String fullName, String email, String phone, String address) {
        account.setFullName(fullName != null ? fullName.trim() : null);
        account.setEmail(email != null ? email.trim() : null);
        account.setPhone(phone != null ? phone.trim() : null);
        account.setAddress(address != null ? address.trim() : null);
        accountRepository.save(account);
    }

    public void changePassword(Account account, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

}
