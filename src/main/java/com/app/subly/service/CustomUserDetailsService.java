package com.app.subly.service;

import com.app.subly.config.AdminConfiguration;
import com.app.subly.model.User;
import com.app.subly.model.UserRole;
import com.app.subly.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final AdminConfiguration adminConfiguration;

    public CustomUserDetailsService(UserRepository repository, PasswordEncoder encoder, AdminConfiguration adminConfiguration){
        this.repository = repository;
        this.encoder = encoder;
        this.adminConfiguration = adminConfiguration;
        this.createDefaultAdminUser();
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
       User user = this.repository.findByUsername(username)
               .orElseThrow(() -> new UsernameNotFoundException("No found user"));

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

    private void createDefaultAdminUser(){
        if(this.repository.findByUsername(this.adminConfiguration.getAdminUsername()).isPresent()){
            return;
        }

        User user = new User();
        user.setUsername(this.adminConfiguration.getAdminUsername());
        user.setName(this.adminConfiguration.getAdminName());
        user.setPasswordHash(this.encoder.encode(this.adminConfiguration.getAdminPassword()));
        user.setEmail(this.adminConfiguration.getAdminEmail());
        user.setRole(UserRole.ADMIN);

        this.repository.save(user);
    }
}
