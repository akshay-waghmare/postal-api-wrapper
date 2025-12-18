package com.mailit.wrapper.controller;

import com.mailit.wrapper.model.RateLimitPlan;
import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.service.ClientService;
import com.mailit.wrapper.service.ClientService.CreateClientResult;

import io.swagger.v3.oas.annotations.Hidden;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal admin controller for client management.
 * 
 * <p>INTERNAL / NON-PUBLIC - These endpoints are not exposed in Swagger
 * and should be protected by infrastructure (network policy, VPN, etc.)</p>
 */
@RestController
@RequestMapping("/admin")
@Hidden // Excludes from Swagger/OpenAPI documentation
public class AdminController {
    
    private final ClientService clientService;
    
    public AdminController(ClientService clientService) {
        this.clientService = clientService;
    }
    
    /**
     * Create a new API client.
     */
    @PostMapping("/clients")
    public ResponseEntity<CreateClientResponse> createClient(
            @Valid @RequestBody CreateClientRequest request) {
        
        CreateClientResult result = clientService.createClient(
                request.name(), 
                request.plan() != null ? request.plan() : RateLimitPlan.FREE
        );
        
        CreateClientResponse response = new CreateClientResponse(
                result.client().getId(),
                result.client().getName(),
                result.client().getApiKeyPrefix(),
                result.rawApiKey(),
                result.client().getPlan()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Rotate an API key for a client.
     */
    @PostMapping("/clients/{clientId}/rotate-key")
    public ResponseEntity<CreateClientResponse> rotateApiKey(
            @PathVariable Long clientId) {
        
        CreateClientResult result = clientService.rotateApiKey(clientId);
        
        CreateClientResponse response = new CreateClientResponse(
                result.client().getId(),
                result.client().getName(),
                result.client().getApiKeyPrefix(),
                result.rawApiKey(),
                result.client().getPlan()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Revoke a client's API key.
     */
    @DeleteMapping("/clients/{clientId}/api-key")
    public ResponseEntity<Void> revokeApiKey(@PathVariable Long clientId) {
        clientService.revokeApiKey(clientId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Update a client's rate limit plan.
     */
    @PatchMapping("/clients/{clientId}/plan")
    public ResponseEntity<ClientDto> updatePlan(
            @PathVariable Long clientId,
            @RequestBody UpdatePlanRequest request) {
        
        Client client = clientService.updatePlan(clientId, request.plan());
        
        ClientDto response = new ClientDto(
                client.getId(),
                client.getName(),
                client.getApiKeyPrefix(),
                client.getPlan()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Request/Response DTOs for admin endpoints
    
    public record CreateClientRequest(
            @NotBlank(message = "Client name is required")
            String name,
            RateLimitPlan plan
    ) {}
    
    public record CreateClientResponse(
            Long id,
            String name,
            String apiKeyPrefix,
            String apiKey, // Raw key - shown only once!
            RateLimitPlan plan
    ) {}
    
    public record UpdatePlanRequest(
            RateLimitPlan plan
    ) {}
    
    public record ClientDto(
            Long id,
            String name,
            String apiKeyPrefix,
            RateLimitPlan plan
    ) {}
}
