package com.aztu.support.dto.common;

/** Simple textual response for actions that don't return a resource. */
public record MessageResponse(String message) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
