package io.mopl.api.user.event;

import java.util.UUID;

public record UserRoleChangedInternalEvent(UUID userId, String userName, String newRole) {}
