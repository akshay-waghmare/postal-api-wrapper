package com.mailit.wrapper.model.dto.response;

/**
 * Single tracking event in the delivery journey.
 * 
 * @param date event timestamp
 * @param status checkpoint status (e.g., "delivered", "transit", "pickup")
 * @param substatus more specific status code (e.g., "delivered001", "transit002")
 * @param description event description
 * @param location event location
 */
public record TrackingEventDto(
        String date,
        String status,
        String substatus,
        String description,
        String location
) {}
