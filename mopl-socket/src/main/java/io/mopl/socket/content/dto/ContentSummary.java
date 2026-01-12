package io.mopl.socket.content.dto;

import io.mopl.socket.content.domain.ContentType;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ContentSummary(
    UUID id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount) {}
