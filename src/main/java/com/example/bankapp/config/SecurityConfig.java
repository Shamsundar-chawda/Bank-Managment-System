package com.example.bankapp.config;

import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer{

    @Autowired
    AccountService accountService;

    @Autowired
    private NavigationSecurityInterceptor navigationSecurityInterceptor;

    @Bean
    public static PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity

                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/admin/dashboard").hasRole("ADMIN")
                        .requestMatchers("/dashboard", "/transactions/**", "/deposit", "/withdraw",
                                "/transfer", "/profile", "/profile/**").hasRole("USER")
                        .requestMatchers("/register", "/login", "/admin", "/error", "/h2-console/**", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) -> {
                    boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication().getAuthorities()
                            .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    if (isAdmin) {
                        response.sendRedirect("/admin/dashboard");
                    } else {
                        response.sendError(org.springframework.http.HttpStatus.FORBIDDEN.value());
                    }
                }).defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/admin"),
                        new AntPathRequestMatcher("/admin/**")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities()
                                    .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
                            response.sendRedirect(isAdmin ? "/admin/dashboard" : "/dashboard");
                        })
                        .failureHandler((request, response, exception) -> {
                            String errorPage = "true".equals(request.getParameter("adminLogin"))
                                    ? "/admin?error" : "/login?error";
                            response.sendRedirect(errorPage);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/login?expired")
                )
                .headers(header -> header
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                );

        return httpSecurity.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(accountService).passwordEncoder(passwordEncoder());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(navigationSecurityInterceptor)
                .addPathPatterns("/dashboard", "/transactions", "/profile");
    }
}
