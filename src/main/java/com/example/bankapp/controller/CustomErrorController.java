package com.example.bankapp.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        int statusCode = 500;
        String errorMessage = "An unexpected error occurred.";

        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (NumberFormatException e) {
                statusCode = 500;
            }

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "The requested URL does not exist or was entered incorrectly.";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "Access denied. You do not have permission to view this resource.";
            } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                errorMessage = "You must be logged in to access this page.";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorMessage = "A server error occurred. Please try again later.";
            }
        }

        // Check whether user is currently logged in
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "error";
    }
}
