package com.app.subly.service;

import com.app.subly.config.AdminConfiguration;
import com.app.subly.entity.user.User;
import com.app.subly.enums.UserRole;
import com.app.subly.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final AdminConfiguration adminConfiguration;

    public CustomUserDetailsService(
            UserRepository userRepository, PasswordEncoder encoder, AdminConfiguration adminConfiguration) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.adminConfiguration = adminConfiguration;
        createDefaultAdminUser();
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No found user"));

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

    private void createDefaultAdminUser() {
        if (userRepository.findByUsername(adminConfiguration.getAdminUsername()).isPresent()) {
            return;
        }

        User user = new User(
                adminConfiguration.getAdminUsername(),
                adminConfiguration.getAdminName(),
                encoder.encode(adminConfiguration.getAdminPassword()),
                adminConfiguration.getAdminEmail(),
                UserRole.ADMIN);

        userRepository.save(user);
    }
}
