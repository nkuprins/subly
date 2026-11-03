package com.app.subly.services;

import com.app.subly.models.User;
import com.app.subly.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository repository;

    public UserService(UserRepository repository){
        this.repository = repository;
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

    // TODO: 1. Implement pushing a new user in the db
    // TODO: 2. Implement getting a user by one id
    // TODO: 3. Implement getting a list of users by their ids
    // TODO: 4. Implement deleting a user by corresponding id
    // TODO: 5. Implement updating a user by its id
}
