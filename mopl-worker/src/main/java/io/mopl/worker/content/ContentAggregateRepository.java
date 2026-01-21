package io.mopl.worker.content;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ContentAggregateRepository {

  private final EntityManager entityManager;

  @Transactional
  public int applyReview(UUID contentId, double rating) {
    // return entityManager
    //     .createNativeQuery(
    //         "UPDATE contents "
    //             + "SET review_count = review_count + 1, "
    //             + "average_rating = (average_rating * review_count + ?1) / (review_count + 1) "
    //             + "WHERE id = ?2")
    //     .setParameter(1, rating)
    //     .setParameter(2, contentId.toString())
    //     .executeUpdate();
    List<Object[]> beforeRows =
        entityManager
            .createNativeQuery("SELECT review_count, average_rating FROM contents WHERE id = ?1")
            .setParameter(1, contentId.toString())
            .getResultList();
    if (beforeRows.isEmpty()) {
      log.warn("applyReview missing content: contentId={}", contentId);
    } else {
      Object[] before = beforeRows.get(0);
      log.info(
          "applyReview before: contentId={}, reviewCount={}, averageRating={}, rating={}",
          contentId,
          before[0],
          before[1],
          rating);
    }

    int updated =
        entityManager
            .createNativeQuery(
                "UPDATE contents "
                    + "SET average_rating = (average_rating * review_count + ?1) / (review_count + 1), "
                    + "review_count = review_count + 1 "
                    + "WHERE id = ?2")
            .setParameter(1, rating)
            .setParameter(2, contentId.toString())
            .executeUpdate();

    if (updated > 0) {
      Object[] row =
          (Object[])
              entityManager
                  .createNativeQuery(
                      "SELECT review_count, average_rating FROM contents WHERE id = ?1")
                  .setParameter(1, contentId.toString())
                  .getSingleResult();
      log.info(
          "applyReview read-back: contentId={}, reviewCount={}, averageRating={}",
          contentId,
          row[0],
          row[1]);
    }

    return updated;
  }
}
