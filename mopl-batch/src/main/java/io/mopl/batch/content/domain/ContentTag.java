package io.mopl.batch.content.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 콘텐츠와 태그의 다대다 관계를 나타내는 조인 엔티티. */
@Entity
@Table(name = "content_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ContentTag {

  @EmbeddedId private ContentTagId id;
}
