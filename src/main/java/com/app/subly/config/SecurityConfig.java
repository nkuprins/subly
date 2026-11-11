package com.app.subly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.ignoringRequestMatchers("/api/**", "/users/delete/me", "/", "/users/logout/me"))
                .authorizeHttpRequests((authorize) -> {
                    authorize
                            .requestMatchers("/users/register")
                            .permitAll()
                            .requestMatchers(
                                    "/users/**",
                                    "/api/category/listAll",
                                    "/api/subscriptions",
                                    "/api/subscriptions/**",
                                    "/api/dashboard/**",
                                    "/api/payments",
                                    "/api/payments/**",
                                    "/dashboard",
                                    "/",
                                    "/api/invitations",
                                    "/api/invitations/**",
                                    "/edit/me",
                                    "/subscriptions/**",
                                    "/providers/**",
                                    "/payments")
                            .authenticated()
                            .requestMatchers("/api/category/**", "/admin", "/admin/**")
                            .hasRole("ADMIN");
                    authorize.anyRequest().permitAll();
                })
                .exceptionHandling((exceptions) -> exceptions.accessDeniedPage("/access-denied"))
                .httpBasic(Customizer.withDefaults())
                .formLogin((form) -> form.loginPage("/login").permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}
