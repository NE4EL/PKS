package ru.mirea.isp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты абонентов. */
public final class SubscriberDtos {
    private SubscriberDtos() {
    }

    public record SubscriberRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 255) String name,
            @Min(2000) @Max(2100) Integer registrationYear,
            @Size(max = 32) String phone,
            @jakarta.validation.constraints.Email @Size(max = 254) String email,
            @Size(max = 500) String address,
            Boolean active,
            Long version) {
    }

    public record SubscriberResponse(
            UUID id,
            String code,
            String name,
            Integer registrationYear,
            String phone,
            String email,
            String address,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}
