package com.mailit.wrapper.service;

import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for API key authentication.
 * 
 * <p>Uses bcrypt to securely compare incoming API keys against
 * stored hashes without exposing the original key.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Validate an API key and return the associated client.
     * 
     * <p>Since we store bcrypt hashes, we cannot look up by hash directly.
     * Instead, we extract the prefix, find potential matches, and verify
     * the full key against each hash.</p>
     * 
     * @param apiKey the raw API key from the request
     * @return the authenticated client, or empty if invalid
     */
    public Optional<Client> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        // Extract prefix (e.g., "sk_live_ab12cd" from "sk_live_ab12cd34ef56gh78...")
        String prefix = extractPrefix(apiKey);
        if (prefix == null) {
            log.debug("API key has invalid format");
            return Optional.empty();
        }

        // Find client by prefix
        Optional<Client> clientOpt = clientRepository.findByApiKeyPrefix(prefix);
        if (clientOpt.isEmpty()) {
            log.debug("No client found for prefix: {}", prefix);
            return Optional.empty();
        }

        Client client = clientOpt.get();

        // Check if client is active
        if (!client.isActive()) {
            log.debug("Client {} is inactive", client.getId());
            return Optional.empty();
        }

        // Check if key is expired
        if (client.getExpiresAt() != null && client.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            log.debug("API key expired for client {} at {}", client.getId(), client.getExpiresAt());
            return Optional.empty();
        }

        // Verify the full API key against the stored hash
        if (passwordEncoder.matches(apiKey, client.getApiKeyHash())) {
            log.trace("API key validated for client: {}", client.getId());
            return Optional.of(client);
        }

        log.debug("API key hash mismatch for client: {}", client.getId());
        return Optional.empty();
    }

    /**
     * Hash an API key for storage.
     * 
     * @param apiKey the raw API key
     * @return bcrypt hash of the key
     */
    public String hashApiKey(String apiKey) {
        return passwordEncoder.encode(apiKey);
    }

    /**
     * Extract the prefix from an API key.
     * 
     * <p>Format: {@code sk_[live|test]_[6-8 chars]}</p>
     * <p>Example: {@code sk_live_ab12cd34ef56...} → {@code sk_live_ab12cd}</p>
     * 
     * @param apiKey the full API key
     * @return the prefix, or null if invalid format
     */
    public String extractPrefix(String apiKey) {
        if (apiKey == null || apiKey.length() < 14) {
            return null;
        }

        // Validate format starts with sk_live_ or sk_test_
        if (!apiKey.startsWith("sk_live_") && !apiKey.startsWith("sk_test_")) {
            return null;
        }

        // Prefix is first 14 characters (sk_live_ + 6 chars)
        // or up to 16 characters (sk_live_ + 8 chars)
        int prefixEnd = Math.min(16, apiKey.length());
        String prefix = apiKey.substring(0, prefixEnd);
        
        // Validate prefix format
        if (!prefix.matches("^sk_(live|test)_[a-z0-9]{6,8}$")) {
            // Try shorter prefix
            prefix = apiKey.substring(0, 14);
            if (!prefix.matches("^sk_(live|test)_[a-z0-9]{6}$")) {
                return null;
            }
        }

        return prefix;
    }

    /**
     * Generate a new API key.
     * 
     * @param isLive true for production key, false for test key
     * @return a new random API key
     */
    public String generateApiKey(boolean isLive) {
        String prefix = isLive ? "sk_live_" : "sk_test_";
        String random = generateRandomString(24);
        return prefix + random;
    }

    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
