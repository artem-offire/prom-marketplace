package ua.marketplace.advertising.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ClickRequest(@NotNull UUID impressionId, @NotBlank @Size(max = 100) String sessionId) {
}
