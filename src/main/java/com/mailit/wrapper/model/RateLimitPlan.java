package com.mailit.wrapper.model;

/**
 * Rate limit plans defining client quotas.
 * 
 * <p>Each plan specifies daily request limits and maximum trackings
 * per batch request. Enterprise plan has unlimited requests.</p>
 */
public enum RateLimitPlan {
    
    /**
     * Free tier - limited for evaluation.
     * 100 requests/day, 10 trackings/batch
     */
    FREE(100, 10),
    
    /**
     * Starter tier - small businesses.
     * 1,000 requests/day, 40 trackings/batch
     */
    STARTER(1_000, 40),
    
    /**
     * Pro tier - medium businesses.
     * 10,000 requests/day, 40 trackings/batch
     */
    PRO(10_000, 40),
    
    /**
     * Enterprise tier - unlimited.
     * Unlimited requests/day, 40 trackings/batch
     */
    ENTERPRISE(-1, 40);

    private final int requestsPerDay;
    private final int trackingsPerBatch;

    RateLimitPlan(int requestsPerDay, int trackingsPerBatch) {
        this.requestsPerDay = requestsPerDay;
        this.trackingsPerBatch = trackingsPerBatch;
    }

    /**
     * Returns the maximum requests per day for this plan.
     * 
     * @return requests per day, or -1 for unlimited
     */
    public int getRequestsPerDay() {
        return requestsPerDay;
    }

    /**
     * Returns the maximum trackings per batch request.
     * 
     * @return max trackings per batch
     */
    public int getTrackingsPerBatch() {
        return trackingsPerBatch;
    }

    /**
     * Checks if this plan has unlimited requests.
     * 
     * @return true if unlimited
     */
    public boolean isUnlimited() {
        return requestsPerDay < 0;
    }
}
