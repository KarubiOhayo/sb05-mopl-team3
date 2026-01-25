package io.mopl.batch.common;

import io.mopl.batch.content.domain.ContentType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ContentDeduplicationTracker {

  private final Set<String> seenKeys = Collections.synchronizedSet(new HashSet<>());

  public boolean isDuplicate(String externalId, ContentType type) {
    if (externalId == null || externalId.isBlank() || type == null) {
      return false;
    }
    String key = type.name() + "|" + externalId;
    return !seenKeys.add(key);
  }
}
