package com.app.subly.controller;

import com.app.subly.dto.subscription.UpdateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.services.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(Principal principal, @Valid @RequestBody CreateSubscriptionRequest request) {
        return service.create(principal.getName(), request);
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse createCustom(Principal principal,
                                             @Valid @RequestBody CreateCustomSubscriptionRequest request) {
        return service.createCustom(principal.getName(), request);
    }

    @GetMapping
    public List<SubscriptionResponse> listMine(Principal principal) {
        return service.findOwnedBy(principal.getName());
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(Principal principal, @PathVariable long id,
                                       @Valid @RequestBody UpdateSubscriptionRequest request) {
        return service.update(principal.getName(), id, request);
    }

    @PutMapping("/{id}/custom")
    public SubscriptionResponse updateCustom(Principal principal, @PathVariable long id,
                                             @Valid @RequestBody UpdateCustomSubscriptionRequest request) {
        return service.updateCustom(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable long id) {
        service.delete(principal.getName(), id);
    }
}
