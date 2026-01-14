package io.mopl.api.content.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mopl.api.content.domain.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

  Optional<Tag> findByName(String name);

  List<Tag> findByNameIn(Collection<String> names);
}
