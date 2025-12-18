package com.mailit.wrapper.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchGetRequest(
    @NotEmpty(message = "Tracking IDs list cannot be empty")
    @Size(max = 40, message = "Cannot fetch more than 40 trackings at once")
    List<String> trackingIds
) {}
