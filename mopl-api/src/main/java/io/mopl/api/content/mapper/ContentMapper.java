package io.mopl.api.content.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import io.mopl.api.content.domain.ContentDocument;
import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.dto.ContentSearchRow;

@Mapper(
	componentModel = "spring",
	unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ContentMapper {

	@Mapping(target = "id", source = "contentId")
	@Mapping(target = "type", source = "type")
	@Mapping(target = "title", source = "title")
	@Mapping(target = "description", source = "description")
	@Mapping(target = "thumbnailImageKey", source = "thumbnailImageKey")
	@Mapping(target = "tags", source = "tags")
	@Mapping(target = "averageRating", source = "averageRating")
	@Mapping(target = "reviewCount", source = "reviewCount")
	@Mapping(target = "watcherCount", source = "watcherCount")
	ContentDto toContentDto(ContentDocument document);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "contentId", source = "id")
	@Mapping(target = "type", source = "type")
	@Mapping(target = "title", source = "title")
	@Mapping(target = "description", source = "description")
	@Mapping(target = "thumbnailImageKey", source = "thumbnailImageKey")
	@Mapping(target = "tags", source = "tags")
	@Mapping(target = "averageRating", source = "averageRating")
	@Mapping(target = "reviewCount", source = "reviewCount")
	@Mapping(target = "watcherCount", source = "watcherCount")
	@Mapping(target = "createdAt", source = "createdAt")
	ContentDocument toContentDocument(ContentSearchRow contentSearchRow);
}
