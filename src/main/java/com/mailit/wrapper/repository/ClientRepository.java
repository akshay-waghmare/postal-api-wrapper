package com.mailit.wrapper.repository;

import com.mailit.wrapper.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Client entity operations.
 * 
 * <p>Provides methods for API key authentication lookups
 * and client management.</p>
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Find a client by their API key hash.
     * Used for authentication - compare bcrypt hash of incoming key.
     * 
     * @param apiKeyHash the bcrypt hash of the API key
     * @return the client if found
     */
    Optional<Client> findByApiKeyHash(String apiKeyHash);

    /**
     * Find a client by their API key prefix.
     * Used for log correlation and debugging.
     * 
     * @param apiKeyPrefix the first 8-16 chars of the API key
     * @return the client if found
     */
    Optional<Client> findByApiKeyPrefix(String apiKeyPrefix);

    /**
     * Check if a client exists with the given API key hash.
     * Fast existence check for authentication.
     * 
     * @param apiKeyHash the bcrypt hash of the API key
     * @return true if client exists
     */
    boolean existsByApiKeyHash(String apiKeyHash);

    /**
     * Find a client by name.
     * 
     * @param name the client's business name
     * @return the client if found
     */
    Optional<Client> findByName(String name);
}
