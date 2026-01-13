package io.mopl.api.conversation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @NotNull(message = "{validation.conversation.with-user-id.required}") UUID withUserId) {}
