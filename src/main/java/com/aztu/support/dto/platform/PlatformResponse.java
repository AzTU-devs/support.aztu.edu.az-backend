package com.aztu.support.dto.platform;

import com.aztu.support.domain.Platform;
import java.time.Instant;

public record PlatformResponse(
        Long id,
        String name,
        String description,
        boolean active,
        boolean additional,
        Instant createdAt) {

    public static PlatformResponse from(Platform platform) {
        return new PlatformResponse(
                platform.getId(),
                platform.getName(),
                platform.getDescription(),
                platform.isActive(),
                platform.isAdditional(),
                platform.getCreatedAt());
    }
}
