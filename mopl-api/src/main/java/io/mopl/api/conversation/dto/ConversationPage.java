package io.mopl.api.conversation.dto;

import io.mopl.api.conversation.domain.Conversation;
import java.util.List;
import java.util.UUID;

public record ConversationPage(
    List<Conversation> conversations, boolean hasNext, String nextCursor, UUID nextIdAfter) {}
