package com.mailit.wrapper.model.entity;

import com.mailit.wrapper.model.WrapperStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * JPA entity representing a shipment tracking record.
 * 
 * <p>Maps wrapper tracking IDs to TrackingMore tracking IDs and stores
 * normalized status information. Supports soft delete for audit trails.</p>
 * 
 * <p>The {@code @SQLRestriction} annotation automatically filters out
 * soft-deleted records from all queries.</p>
 */
@Entity
@Table(name = "trackings")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Wrapper-generated tracking ID exposed to clients.
     * Format: trk_[a-z0-9]{8,16} (e.g., "trk_9f3a2b8c")
     */
    @Column(name = "tracking_id", nullable = false, unique = true, length = 32)
    @Pattern(regexp = "^trk_[a-z0-9]{8,16}$", 
             message = "Tracking ID must match format: trk_xxxxxxxx")
    private String trackingId;

    /**
     * The client who owns this tracking record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * Original shipment tracking number from the carrier.
     */
    @Column(name = "tracking_number", nullable = false)
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    /**
     * Courier/carrier code (e.g., "india-post", "usps").
     * Lowercase alphanumeric with hyphens.
     */
    @Column(name = "courier_code", nullable = false, length = 100)
    @Pattern(regexp = "^[a-z0-9-]+$", 
             message = "Courier code must be lowercase alphanumeric with hyphens")
    private String courierCode;

    /**
     * TrackingMore's internal tracking ID.
     * Populated after successful upstream registration.
     */
    @Column(name = "trackingmore_id", unique = true)
    private String trackingmoreId;

    /**
     * ISO 3166-1 alpha-2 origin country code.
     */
    @Column(name = "origin_country", length = 2)
    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    private String originCountry;

    /**
     * ISO 3166-1 alpha-2 destination country code.
     */
    @Column(name = "destination_country", length = 2)
    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    private String destinationCountry;

    /**
     * Normalized tracking status.
     */
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private WrapperStatus status;

    /**
     * Client's internal order ID for reference.
     */
    @Column(name = "order_id")
    private String orderId;

    /**
     * Timestamp when tracking was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Timestamp of last tracking update.
     */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Soft delete timestamp. NULL if active.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Performs a soft delete by setting deletedAt timestamp.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Checks if this tracking has been soft-deleted.
     * 
     * @return true if deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Checks if this tracking is in a final state (no further updates expected).
     * 
     * @return true if status is DELIVERED, EXPIRED, or RETURNED
     */
    public boolean isFinalState() {
        return status == WrapperStatus.DELIVERED 
            || status == WrapperStatus.EXPIRED 
            || status == WrapperStatus.RETURNED;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }
}
