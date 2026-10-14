package com.example.bankapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class NavigationSecurityInterceptor implements HandlerInterceptor {

    private static final Set<String> GUARDED_PATHS = Set.of(
            "/dashboard",
            "/transactions",
            "/profile"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Only guard customer banking pages
        if (!GUARDED_PATHS.contains(uri)) {
            return true;
        }

        // Only enforce for authenticated users
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        HttpSession session = request.getSession(true);
        String lastAllowedPage = (String) session.getAttribute("CURRENT_PAGE");

        String navParam = request.getParameter("nav");
        String secFetchSite = request.getHeader("Sec-Fetch-Site");
        String referer = request.getHeader("Referer");

        boolean hasClickToken = "click".equalsIgnoreCase(navParam) || "1".equals(navParam);
        boolean isBrowserInAppClick = "same-origin".equalsIgnoreCase(secFetchSite) && referer != null;
        boolean isFromAuthFlow = referer != null && (referer.contains("/login") || referer.contains("/register"));
        boolean isFormPostRedirect = (referer != null && (referer.contains("/deposit") || referer.contains("/withdraw") || referer.contains("/transfer") || referer.contains("/profile")));
        boolean isSamePageReload = (lastAllowedPage != null && lastAllowedPage.equals(uri));

        // Allow legitimate in-app transitions:
        // 1. Initial login landing
        // 2. Explicit icon/button click token (?nav=click)
        // 3. Post-action form redirects (after deposit, transfer, profile update)
        // 4. Same page reload / refresh
        // 5. In-app navigation with same-origin referer
        if (lastAllowedPage == null || isFromAuthFlow || hasClickToken || isFormPostRedirect || isSamePageReload || isBrowserInAppClick) {
            session.setAttribute("CURRENT_PAGE", uri);
            return true;
        }

        // Direct manual typing in the address bar detected!
        // Block manual navigation and redirect back to the user's active page
        String redirectTarget = (lastAllowedPage != null) ? lastAllowedPage : "/dashboard";
        if (redirectTarget.equals(uri)) {
            redirectTarget = "/dashboard";
        }

        response.sendRedirect(redirectTarget + "?security_alert=direct_url_blocked");
        return false;
    }
}
