package io.mopl.batch.common.event;

import java.util.List;
import java.util.UUID;

public record ContentIndexBatchSpringEvent(List<UUID> contentIds) {}
