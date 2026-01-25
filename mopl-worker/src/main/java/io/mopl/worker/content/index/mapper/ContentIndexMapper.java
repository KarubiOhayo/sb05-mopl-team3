package io.mopl.worker.content.index.mapper;

import io.mopl.worker.content.index.domain.ContentDocument;
import io.mopl.worker.content.index.dto.ContentIndexRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ContentIndexMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "contentId", source = "id")
  ContentDocument toDocument(ContentIndexRow row);
}
