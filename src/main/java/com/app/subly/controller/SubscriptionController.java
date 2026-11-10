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

    @GetMapping("/{id}/members")
    public List<SubscriptionMemberResponse> listMembers(Principal principal, @PathVariable @Positive long id) {

        return service.findMembers(principal.getName(), id);
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

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(Principal principal, @PathVariable @Positive long id) {

        service.leave(principal.getName(), id);
    }

    @DeleteMapping("/{id}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            Principal principal, @PathVariable @Positive long id, @PathVariable @Positive long membershipId) {

        service.removeMember(principal.getName(), id, membershipId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable @Positive long id) {
        service.delete(principal.getName(), id);
    }
}
