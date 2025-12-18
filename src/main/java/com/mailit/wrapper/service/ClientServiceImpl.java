package com.mailit.wrapper.service;

import com.mailit.wrapper.exception.TrackingNotFoundException;
import com.mailit.wrapper.model.RateLimitPlan;
import com.mailit.wrapper.model.entity.Client;
import com.mailit.wrapper.repository.ClientRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Implementation of client management operations.
 * 
 * <p>Generates Stripe-style API keys with the format:
 * {@code sk_live_[prefix]_[secret]}</p>
 * 
 * <p>The prefix is stored in plaintext for quick lookup, while the full
 * key is hashed with bcrypt for secure storage.</p>
 */
@Service
public class ClientServiceImpl implements ClientService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);
    
    private static final String KEY_PREFIX = "sk_live_";
    private static final int PREFIX_LENGTH = 8;
    private static final int SECRET_LENGTH = 32;
    
    private final ClientRepository clientRepository;
    private final SecureRandom secureRandom;
    
    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
        this.secureRandom = new SecureRandom();
    }
    
    @Override
    @Transactional
    public CreateClientResult createClient(String name, RateLimitPlan plan) {
        // Generate API key components
        String prefixPart = generatePrefix();
        String secret = generateSecret();
        String fullPrefix = KEY_PREFIX + prefixPart;
        String rawApiKey = fullPrefix + "_" + secret;
        String keyHash = BCrypt.hashpw(rawApiKey, BCrypt.gensalt(12));
        
        // Create client
        Client client = new Client();
        client.setName(name);
        client.setApiKeyPrefix(fullPrefix);
        client.setApiKeyHash(keyHash);
        client.setPlan(plan);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        
        client = clientRepository.save(client);
        logger.info("Created client {} with prefix {}", client.getId(), fullPrefix);
        
        return new CreateClientResult(client, rawApiKey);
    }
    
    @Override
    @Transactional
    public CreateClientResult rotateApiKey(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new TrackingNotFoundException("Client not found: " + clientId));
        
        // Generate new API key
        String prefixPart = generatePrefix();
        String secret = generateSecret();
        String fullPrefix = KEY_PREFIX + prefixPart;
        String rawApiKey = fullPrefix + "_" + secret;
        String keyHash = BCrypt.hashpw(rawApiKey, BCrypt.gensalt(12));
        
        // Update client
        String oldPrefix = client.getApiKeyPrefix();
        client.setApiKeyPrefix(fullPrefix);
        client.setApiKeyHash(keyHash);
        client.setUpdatedAt(LocalDateTime.now());
        
        client = clientRepository.save(client);
        logger.info("Rotated API key for client {} (old prefix: {}, new prefix: {})", 
                client.getId(), oldPrefix, fullPrefix);
        
        return new CreateClientResult(client, rawApiKey);
    }
    
    @Override
    @Transactional
    public void revokeApiKey(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new TrackingNotFoundException("Client not found: " + clientId));
        
        // Revoke by clearing the hash (prefix kept for audit)
        String prefix = client.getApiKeyPrefix();
        client.setApiKeyHash(null);
        client.setUpdatedAt(LocalDateTime.now());
        
        clientRepository.save(client);
        logger.info("Revoked API key for client {} (prefix: {})", clientId, prefix);
    }
    
    @Override
    @Transactional
    public Client updatePlan(Long clientId, RateLimitPlan plan) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new TrackingNotFoundException("Client not found: " + clientId));
        
        RateLimitPlan oldPlan = client.getPlan();
        client.setPlan(plan);
        client.setUpdatedAt(LocalDateTime.now());
        
        client = clientRepository.save(client);
        logger.info("Updated plan for client {} from {} to {}", clientId, oldPlan, plan);
        
        return client;
    }

    @Override
    @Transactional
    public Client updateStatus(Long clientId, boolean active, LocalDateTime expiresAt) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new TrackingNotFoundException("Client not found: " + clientId));
        
        client.setActive(active);
        client.setExpiresAt(expiresAt);
        client.setUpdatedAt(LocalDateTime.now());
        
        client = clientRepository.save(client);
        logger.info("Updated status for client {}: active={}, expiresAt={}", clientId, active, expiresAt);
        
        return client;
    }

    @Override
    public java.util.List<Client> getAllClients() {
        return clientRepository.findAll();
    }
    
    private String generatePrefix() {
        byte[] bytes = new byte[PREFIX_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bytes)
                .substring(0, PREFIX_LENGTH)
                .toLowerCase();
    }
    
    private String generateSecret() {
        byte[] bytes = new byte[SECRET_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bytes)
                .substring(0, SECRET_LENGTH);
    }
}
