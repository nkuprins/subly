package com.app.subly.controller;

import com.app.subly.dto.subscription.*;
import com.app.subly.service.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public List<VisibleSubscriptionResponse> listMine(Principal principal) {
        return service.findCurrent(principal.getName());
    }

    @GetMapping("/cancelled")
    public List<CancelledSubscriptionResponse> listCancelled(Principal principal) {
        return service.findCancelled(principal.getName());
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(
            Principal principal,
            @PathVariable @Positive long id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        return service.update(principal.getName(), id, request);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(Principal principal, @PathVariable @Positive long id) {
        service.cancel(principal.getName(), id);
    }

    @PatchMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivate(Principal principal, @PathVariable @Positive long id) {
        service.reactivate(principal.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable @Positive long id) {
        service.delete(principal.getName(), id);
    }
}
