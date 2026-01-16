package io.mopl.socket.dm.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectMessageSendRequest(
    @NotBlank(message = "{validation.content.blank}") String content) {}
