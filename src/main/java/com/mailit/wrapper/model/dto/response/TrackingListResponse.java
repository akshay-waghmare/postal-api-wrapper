package com.mailit.wrapper.model.dto.response;

import java.util.List;

/**
 * Paginated list of trackings.
 * 
 * @param trackings list of tracking summaries
 * @param pagination pagination metadata
 */
public record TrackingListResponse(
        List<TrackingSummaryDto> trackings,
        PaginationMeta pagination
) {}
