package com.example.bankapp.controller;

import com.example.bankapp.service.AccountService;
import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "bank.admin.username=test-admin",
        "bank.admin.password=test-admin-password",
        "spring.datasource.url=jdbc:h2:mem:admin-portal-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminPortalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void adminCanSignInAndReachSimpleAdminLandingPage() throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .param("adminLogin", "true")
                        .param("username", "test-admin")
                        .param("password", "test-admin-password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BankEase admin portal")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Customer accounts")));
    }

    @Test
    void adminDashboardRendersCustomerAccountInformationWithoutSuspensionControls() throws Exception {
        String username = "dashboard-customer-" + UUID.randomUUID();
        accountService.registerAccount(username, "customer-password");
        Account account = accountService.findAccountByUsername(username);
        account.setBalance(null);
        accountRepository.save(account);

        mockMvc.perform(get("/admin/dashboard").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(username)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("$0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account overview")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Suspend account"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/admin/customers/"))));

        accountRepository.deleteById(account.getId());
    }

    @Test
    void adminLoginPageIsAvailableAndRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(post("/login")
                        .param("adminLogin", "true")
                        .param("username", "test-admin")
                        .param("password", "incorrect")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?error"));
    }

    @Test
    void unauthenticatedAdminDashboardRequestReturnsToAdminSignIn() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/admin"));
    }

    @Test
    void signedInAdminOpeningAdminSignInIsReturnedToDashboard() throws Exception {
        mockMvc.perform(get("/admin").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void customerCannotOpenAdminLandingPage() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("customer").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDoesNotReceiveCustomerDashboard() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void errorPageShowsWorkingAdminNavigation() throws Exception {
        mockMvc.perform(get("/error").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/dashboard\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("href=\"/profile"))));
    }

    @Test
    void errorPageUsesAllowedCustomerNavigationLinks() throws Exception {
        mockMvc.perform(get("/error").with(user("test-customer").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/dashboard?nav=click\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/profile?nav=click\"")));
    }

    @Test
    void adminUsernameCannotBeRegisteredAsCustomer() {
        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> accountService.registerAccount("test-admin", "customer-password"));
    }

    @Test
    void accountSuspensionEndpointIsRemoved() throws Exception {
        mockMvc.perform(post("/admin/customers/1234567890/suspension")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .param("suspended", "true"))
                .andExpect(status().isNotFound());
    }
}
