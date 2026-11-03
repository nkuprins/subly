package com.app.subly.services;

import com.app.subly.exceptions.UserAlreadyExistsException;
import com.app.subly.models.User;
import com.app.subly.models.UserRole;
import com.app.subly.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    // TODO: 2. Implement getting a user by one id
    // TODO: 3. Implement getting a list of users by their ids
    // TODO: 4. Implement deleting a user by corresponding id
    // TODO: 5. Implement updating a user by its id

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder){
        this.repository = repository;
        this.encoder = encoder;
    }

    public void addNewUser(String username, String password, String email){
        try{
            this.repository.save(this.createUserFromRawData(username, password, email));
        } catch(DataIntegrityViolationException exception){
            throw new UserAlreadyExistsException("A user with the username or the email already exists");
        }
    }

    private User createUserFromRawData(String username, String password, String email){
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setEmail(email);
        user.setRole(UserRole.USER);
        return user;
    }
}
