package ru.mirea.studenthub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты учебных групп. */
public final class GroupDtos {
    private GroupDtos() {
    }

    public record GroupRequest(
            @NotBlank @Size(max = 50) String code,
            @Size(max = 200) String name,
            @Min(2000) @Max(2100) Integer admissionYear,
            Boolean active,
            Long version) {
    }

    public record GroupResponse(
            UUID id,
            String code,
            String name,
            Integer admissionYear,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}
