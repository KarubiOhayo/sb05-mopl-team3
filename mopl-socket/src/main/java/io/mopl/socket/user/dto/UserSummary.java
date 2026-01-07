package io.mopl.socket.user.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record UserSummary(UUID userId, String name, String profileImageUrl) {}
