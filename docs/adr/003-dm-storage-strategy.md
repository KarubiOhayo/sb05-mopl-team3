# ADR 003 - DM 저장 전략

## 상태
- Proposed

## 배경
- DM은 유실 허용이 낮고 읽음 처리와 정합성 요구가 높다.
- 대규모 트래픽을 전제로 확장성도 고려해야 한다.
- 내부 통신은 Kafka 또는 Redis를 사용할 수 있다.
- 포트폴리오 관점에서 의사결정 근거와 성능 검증이 중요하다.

## 결정
- DM 저장은 Kafka 소비 후 DB에 즉시 저장을 기본으로 하되, DB writer는 배치 insert로 최적화한다.
- 구현은 다음 단계로 나눈다.
  - Phase 1: Kafka 소비 즉시 단건 insert
  - Phase 2: Kafka 소비 즉시 저장 로직에 batch insert 도입

## 대안
- 진짜 배치 저장(시간/건수 기준 DB 플러시)
  - 장점: 최대 처리량 향상
  - 단점: 최신성 저하, 장애 시 유실 및 재처리 복잡, 읽음 처리와 정렬 보장 어려움
- Redis Write Behind
  - 장점: 높은 처리량, DB 부하 분산
  - 단점: Redis 장애/eviction 시 유실 가능, AOF 정책 및 재처리 설계 필요
- 즉시 저장(단건 insert)
  - 장점: 단순하고 정합성 높음
  - 단점: 고트래픽에서 DB 병목 우려

## 근거
- DM은 유실 허용이 낮아 즉시 저장이 기본적으로 안전하다.
- DB 병목은 batch insert로 완화 가능하며 구현 난이도 대비 효과가 크다.
- 진짜 배치 저장이나 Redis Write Behind는 일관성 비용과 운영 복잡도가 크다.

## 결과
- 장점
  - 정합성 유지와 유실 리스크 최소화
  - 배치 insert로 성능 개선 여지 확보
- 단점
  - 진짜 배치 저장 대비 최대 처리량은 낮음
  - batch insert 적용을 위한 추가 구현 필요

## 후속 작업
- k6 + Prometheus + Grafana로 성능 측정
- 즉시 저장 vs batch insert의 TPS/Latency/DB 부하 비교
- 결과는 벤치마크 문서로 정리
