package com.app.subly.controller;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.service.ProviderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderResponse create(Principal principal, @Valid @RequestBody ProviderRequest request) {
        return service.create(principal.getName(), request);
    }

    @GetMapping
    public List<ProviderResponse> listVisible(Principal principal) {
        return service.findVisibleTo(principal.getName());
    }

    @PutMapping("/{id}")
    public ProviderResponse update(
            Principal principal, @PathVariable @Positive long id, @Valid @RequestBody ProviderRequest request) {
        return service.updateCustomProvider(principal.getName(), id, request);
    }
}
