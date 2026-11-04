package com.app.subly.services;

import com.app.subly.models.User;
import com.app.subly.models.UserRole;
import com.app.subly.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public CustomUserDetailsService(UserRepository repository, PasswordEncoder encoder){
        this.repository = repository;
        this.encoder = encoder;
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
        if(this.repository.findByUsername("admin").isPresent()){
            return;
        }

        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash(this.encoder.encode("test_admin_subly_12345"));
        user.setEmail("admin@admin.com");
        user.setRole(UserRole.ADMIN);

        this.repository.save(user);
    }
}
