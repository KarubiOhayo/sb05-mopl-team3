package io.mopl.api.content.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.mopl.api.content.domain.ContentDocument;
import io.mopl.api.content.domain.ContentElasticRepository;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.domain.EventType;
import io.mopl.api.content.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentElasticEventListener {

	private final ContentRepository contentRepository;
	private final ContentElasticRepository contentElasticRepository;
	private final ContentMapper contentMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ContentIndexEvent event) {

		if (event.getType() == EventType.DELETE) {
			log.info("Elastic Search에서 삭제할 컨텐츠 - contentId={}", event.getContentId());
			contentElasticRepository.deleteByContentId(event.getContentId());
			return;
		}

		log.info("Elastic Search에서 upsert할 컨텐츠 - contentId={}", event.getContentId());
		ContentDocument doc = contentRepository.findOneForIndexing(event.getContentId())
			.map(contentMapper::toContentDocument)
			.orElse(null);

		if (doc == null) {
			contentElasticRepository.deleteByContentId(event.getContentId());
			return;
		}

		contentElasticRepository.findByContentId(event.getContentId())
			.ifPresent(existing -> doc.setId(existing.getId()));

		contentElasticRepository.save(doc);
		log.info("Elastic Search에 저장 완료 - contentId={}", doc.getContentId());
	}
}
