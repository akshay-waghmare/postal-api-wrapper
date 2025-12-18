package com.mailit.wrapper.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique, URL-safe tracking IDs.
 * 
 * <p>Format: {@code trk_[a-z0-9]{12}} (e.g., "trk_9f3a2b8c1d2e")</p>
 * 
 * <p>Uses SecureRandom for collision resistance. With 12 alphanumeric
 * characters (36^12 combinations), collision probability is negligible
 * even at billions of trackings.</p>
 */
@Component
public class TrackingIdGenerator {

    private static final String PREFIX = "trk_";
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a new unique tracking ID.
     * 
     * @return tracking ID in format trk_xxxxxxxxxxxx
     */
    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX.length() + ID_LENGTH);
        sb.append(PREFIX);
        
        for (int i = 0; i < ID_LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        
        return sb.toString();
    }

    /**
     * Validate that a string is a valid tracking ID format.
     * 
     * @param trackingId the ID to validate
     * @return true if valid format
     */
    public boolean isValid(String trackingId) {
        if (trackingId == null || !trackingId.startsWith(PREFIX)) {
            return false;
        }
        
        String idPart = trackingId.substring(PREFIX.length());
        if (idPart.length() < 8 || idPart.length() > 16) {
            return false;
        }
        
        for (char c : idPart.toCharArray()) {
            if (ALPHABET.indexOf(c) < 0) {
                return false;
            }
        }
        
        return true;
    }
}
