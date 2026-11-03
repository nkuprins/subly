package com.app.subly.controller;

import com.app.subly.dto.CreateUserRequest;
import com.app.subly.dto.OperationResponse;
import com.app.subly.exceptions.UserAlreadyExistsException;
import com.app.subly.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    // TODO: 2. get one user
    // TODO: 3. get list of users
    // TODO: 4. delete a user
    // TODO: 5. update a user
    private final UserService service;

    public AuthController(UserService service){
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<OperationResponse> createNewUser(@Valid @RequestBody CreateUserRequest request){
        try{
            this.service.addNewUser(request.getUsername(), request.getPassword(), request.getEmail());
            return ResponseEntity.ok(new OperationResponse(false, "The user has been registered successfully"));
        }catch(UserAlreadyExistsException exception){
            return ResponseEntity.badRequest().body(new OperationResponse(true, exception.getMessage()));
        }
    }
}
