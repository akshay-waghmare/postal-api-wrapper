package com.mailit.wrapper.model.entity;

import com.mailit.wrapper.model.RateLimitPlan;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing an API client.
 * 
 * <p>Clients authenticate via API keys stored using Stripe-style pattern:
 * a plain-text prefix for log correlation and a bcrypt hash for secure
 * authentication.</p>
 * 
 * <p>Example API key: {@code sk_live_ab12cd34ef56gh78}</p>
 * <ul>
 *   <li>Prefix stored: {@code sk_live_ab12cd}</li>
 *   <li>Hash stored: bcrypt hash of full key</li>
 * </ul>
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client's business name.
     */
    @Column(nullable = false)
    @NotBlank(message = "Client name is required")
    private String name;

    /**
     * API key prefix for log correlation (e.g., "sk_live_ab12cd").
     * Visible in logs but doesn't expose full key.
     */
    @Column(name = "api_key_prefix", nullable = false, length = 16)
    @Pattern(regexp = "^sk_(live|test)_[a-z0-9]{6,8}$", 
             message = "API key prefix must match pattern: sk_live_xxxxx or sk_test_xxxxx")
    private String apiKeyPrefix;

    /**
     * bcrypt hash of the full API key.
     * Used for secure authentication without storing plaintext key.
     */
    @Column(name = "api_key_hash", nullable = false, unique = true, length = 60)
    private String apiKeyHash;

    /**
     * Rate limit plan determining request quotas.
     */
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RateLimitPlan plan = RateLimitPlan.FREE;

    /**
     * Timestamp when the client was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp of last client update.
     */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Updates the updatedAt timestamp before persist/update.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
}
