package io.mopl.api.content.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mopl.api.content.domain.Content;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentQueryRepository {}
