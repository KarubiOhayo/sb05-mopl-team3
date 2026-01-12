package io.mopl.socket.watching.dto;

import lombok.Builder;

@Builder
public record WatchingSessionChange(
    ChangeType type, WatchingSessionDto watchingSession, long watcherCount) {}
