package com.mailit.wrapper.service;

import com.mailit.wrapper.model.RateLimitPlan;
import com.mailit.wrapper.model.entity.Client;

/**
 * Service interface for client management operations.
 * 
 * <p>Used by administrators to manage API clients and their API keys.</p>
 */
public interface ClientService {
    
    /**
     * Create a new client with an API key.
     * 
     * <p>Generates a Stripe-style API key in the format:
     * {@code sk_live_[prefix]_[secret]}</p>
     * 
     * @param name the client name
     * @param plan the rate limit plan
     * @return result containing the client and the raw API key (shown once)
     */
    CreateClientResult createClient(String name, RateLimitPlan plan);
    
    /**
     * Rotate an API key for an existing client.
     * 
     * <p>The old key is immediately invalidated and a new one is generated.</p>
     * 
     * @param clientId the client ID
     * @return result containing the client and the new raw API key
     */
    CreateClientResult rotateApiKey(Long clientId);
    
    /**
     * Revoke a client's API key.
     * 
     * <p>The client's API key is invalidated. They will need a new key
     * to access the API.</p>
     * 
     * @param clientId the client ID
     */
    void revokeApiKey(Long clientId);
    
    /**
     * Update a client's rate limit plan.
     * 
     * @param clientId the client ID
     * @param plan the new rate limit plan
     * @return the updated client
     */
    Client updatePlan(Long clientId, RateLimitPlan plan);

    /**
     * Update a client's status and expiration.
     * 
     * @param clientId the client ID
     * @param active whether the client is active
     * @param expiresAt optional expiration date (null to clear)
     * @return the updated client
     */
    Client updateStatus(Long clientId, boolean active, java.time.LocalDateTime expiresAt);

    /**
     * List all clients.
     * 
     * @return list of all clients
     */
    java.util.List<Client> getAllClients();
    
    /**
     * Result of creating or rotating an API key.
     * 
     * <p>Contains the raw API key which should only be shown once to the user.</p>
     * 
     * @param client the client entity
     * @param rawApiKey the raw API key (only shown once)
     */
    record CreateClientResult(Client client, String rawApiKey) {}
}
