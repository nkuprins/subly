package com.app.subly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf)
                        -> csrf.ignoringRequestMatchers(
                                "/api/**", "/users/delete/me", "/", "/users/logout/me" )).
                authorizeHttpRequests((authorize) -> {
                    authorize.requestMatchers("/users/register").permitAll()
                            .requestMatchers("/users/**").authenticated()
                            .requestMatchers("/api/category/listAll").authenticated()
                            .requestMatchers("/api/category/**", "/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/subscriptions",
                                    "/api/subscriptions/**",
                                    "/api/dashboard/**",
                                    "/dashboard",
                                    "/",
                                    "/api/invitations",
                                    "/api/invitations/**")
                            .authenticated();
                    authorize.anyRequest().permitAll();
                }).exceptionHandling((ex) -> ex.accessDeniedPage("/access-denied"))
                .httpBasic(Customizer.withDefaults())
                .formLogin((form) -> form.loginPage("/login").permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}
