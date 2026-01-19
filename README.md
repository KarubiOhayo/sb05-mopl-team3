# 🎬 모두의 플리 (MOPL)

<div align="center">

### 대규모 트래픽을 고려한 글로벌 콘텐츠 큐레이션 & 소셜 플랫폼

[![Build Status](https://img.shields.io/github/actions/workflow/status/KarubiOhayo/sb05-mopl-team3/ci.yml?branch=main&style=flat-square)](https://github.com/KarubiOhayo/sb05-mopl-team3/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)

[🌐 데모 사이트](https://project.sb.sprint.learn.codeit.kr/sb/mopl/) | [📖 협업 문서 ](https://www.notion.so/Project-Home-2cdfdef79f2d8073ae4ee3d060a0361a?source=copy_link)

</div>

---

## 💡 프로젝트 소개

**모두의 플리**는 영화, 드라마, 스포츠 등 다양한 콘텐츠를 **큐레이팅하고 공유**하며, **실시간으로 함께 시청**할 수 있는 소셜 플랫폼입니다.

사용자는 자신만의 플레이리스트를 만들고, 다른 사용자와 소통하며 콘텐츠 경험을 확장할 수 있습니다. 대규모 트래픽을 고려한 확장 가능한 아키텍처로 설계되어, 안정적이고 빠른 서비스를 제공합니다.

### 🎯 핵심 가치

- **🔐 안전한 사용자 관리**: JWT 기반 인증과 OAuth2 소셜 로그인 (Google, Kakao)
- **📝 개인화된 큐레이션**: 사용자 맞춤형 플레이리스트 생성 및 관리
- **⚡ 실시간 소통**: WebSocket 기반 실시간 시청 세션 및 DM
- **🔍 강력한 검색**: Elasticsearch를 활용한 Full-Text Search
- **📊 확장 가능한 설계**: 멀티모듈 아키텍처와 이벤트 기반 비동기 처리
- **☁️ 클라우드 네이티브**: AWS ECS 기반 컨테이너 배포 및 자동 확장

### ✨ 주요 특징

```
🎭 콘텐츠 큐레이션    │  나만의 플레이리스트 생성 및 공유
⭐ 리뷰 시스템       │  콘텐츠 평가 및 커뮤니티 피드백
👥 소셜 네트워킹     │  팔로우, DM, 실시간 채팅
📺 함께 보기         │  WebSocket 기반 실시간 시청 세션
🔔 실시간 알림       │  SSE를 통한 즉각적인 알림 전달
🛡️ 관리자 기능       │  사용자 권한 관리 및 콘텐츠 관리
```

### 🏗️ 프로젝트 특징

- **멀티모듈 아키텍처**: API, Socket, Worker, Batch 모듈 독립 배포
- **이벤트 기반 설계**: Kafka를 통한 모듈 간 느슨한 결합
- **고성능 캐싱**: Redis를 활용한 세션 관리 및 데이터 캐싱
- **CI/CD 자동화**: GitHub Actions를 통한 자동 테스트 및 배포

---

## 🎬 데모 계정

프로토타입에서 다음 계정으로 서비스를 체험해보실 수 있습니다:

| 닉네임 | 이메일 | 비밀번호 | 권한 |
|--------|--------|----------|------|
| admin | `admin@mopl.io` | `1234` | ADMIN |
| 우디 | `woody@mopl.io` | `1234` | USER |
| 버즈 | `buzz@mopl.io` | `1234` | USER |
| 제시 | `jessie@mopl.io` | `1234` | USER |
| 렉스 | `rex@mopl.io` | `1234` | USER |
| 슬링키 | `slinky@mopl.io` | `1234` | USER |

---

## 👥 팀원 및 역할 (R&R)

> 💻 **Backend Development Team** - 전원 백엔드 개발자로 구성된 팀입니다.

<div align="center">

| <img src="https://github.com/KarubiOhayo.png" width="120" height="120"/> | <img src="https://github.com/kjn4101.png" width="120" height="120"/> | <img src="https://github.com/chanhyeok0201.png" width="120" height="120"/> | <img src="https://github.com/rlawnsry.png" width="120" height="120"/> | <img src="https://github.com/woohyuk99.png" width="120" height="120"/> |
|:---:|:---:|:---:|:---:|:---:|
| **김규섭**<br/>🎯 *Team Leader* | **권지인**<br/>💻 *Developer* | **김찬혁**<br/>💻 *Developer* | **김준교**<br/>💻 *Developer* | **변우혁**<br/>💻 *Developer*<br/> |
| **Worker 모듈**<br/>Event Processing<br/>**Socket 모듈**<br/>Real-time<br/>**Batch 모듈**<br/>Scheduling | **API 모듈**<br/>Auth, User<br/>**Infrastructure 모듈**<br/>Redis | **API 모듈**<br/>Review<br/>**Socket 모듈**<br/>Real-time | **API 모듈**<br/>Contents | **API 모듈**<br/>Review |
| 여기에 들어갈 작업내용 정리 부탁합니다 | ㅁㅁ | ㄴㄴ | ㄷㄷ | ㄹㄹ |

</div>

### 📋 공통 책임
- **코드 리뷰**: 모든 PR의 SonarQube 및 CodeRabbit의 정적 분석 도구를 이용해 코드 리뷰 진행
- **문서화**: API 명세, 기술 문서, Trouble Shooting 기록
- **데일리 스크럼**: 매일 진행 상황 및 블로커 공유

### 🔄 협업 프로세스
- **브랜치 전략**: Git Flow (main, develop, feature/*, hotfix/*)
- **커밋 컨벤션**: Conventional Commits
- **이슈 관리**: GitHub Issues + Projects
- **커뮤니케이션**: Zep, Discord, notion

---

## 📸 스크린샷

> HTTPS 배포 끝나면 메인 화면 등 이것저것 캡처해서 올려두거나... 아니면 아예 시연 영상 여기다 넣어두거나... 

<div align="center">
  <img src="docs/images/screenshot-main.png" alt="메인 화면" width="45%">
  <img src="docs/images/screenshot-playlist.png" alt="플레이리스트" width="45%">
</div>

---
## 🛠️ 기술 스택

### 🔧 Backend Framework

| 기술 | 버전 | 용도 |
|------|------|------|
| ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white) | 3.2+ | 메인 애플리케이션 프레임워크 |
| ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white) | 6.x | 인증/인가 및 보안 |
| ![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white) | 5.x | 배치 작업 처리 |

### 🔐 Authentication & Authorization

| 기술 | 용도 |
|------|------|
| ![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white) | 토큰 기반 인증 |
| OAuth2 | 소셜 로그인 (Google, Kakao) |
| Spring Security OAuth2 | OAuth2 인증 서버 구현 |

### 💾 Database & ORM

| 기술 | 버전 | 용도 |
|------|------|------|
| ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white) | 8.0+ | 관계형 데이터베이스 |
| ![JPA](https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white) | - | ORM 표준 |
| ![QueryDSL](https://img.shields.io/badge/QueryDSL-0078D4?style=for-the-badge&logo=java&logoColor=white) | - | 타입 안전 쿼리 DSL |

### 🚀 Cache & Session

| 기술 | 버전 | 용도 |
|------|------|------|
| ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white) | 7.x | 캐싱 및 세션 관리 |
| Spring Data Redis | - | Redis 연동 |

### 📨 Message Queue

| 기술 | 버전 | 용도 |
|------|------|------|
| ![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white) | 3.x | 이벤트 스트리밍 플랫폼 |
| Spring Kafka | - | Kafka 연동 |
| Confluent Cloud | - | 관리형 Kafka (선택사항) |

### 🔍 Search Engine

| 기술 | 버전 | 용도 |
|------|------|------|
| ![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white) | 8.x | Full-Text Search 엔진 |
| Spring Data Elasticsearch | - | Elasticsearch 연동 |

### ⚡ Real-time Communication

| 기술 | 용도 |
|------|------|
| ![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white) | 양방향 실시간 통신 |
| STOMP | WebSocket 메시징 프로토콜 |
| SSE (Server-Sent Events) | 서버 → 클라이언트 단방향 실시간 전송 |

### ☁️ Cloud Infrastructure (AWS)

| 서비스 | 용도 |
|--------|------|
| ![AWS ECS](https://img.shields.io/badge/AWS%20ECS-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white) | 컨테이너 오케스트레이션 (Fargate) |
| ![AWS ECR](https://img.shields.io/badge/AWS%20ECR-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white) | Docker 이미지 레지스트리 |
| ![AWS RDS](https://img.shields.io/badge/AWS%20RDS-527FFF?style=for-the-badge&logo=amazon-rds&logoColor=white) | 관리형 MySQL 데이터베이스 |
| ![AWS ElastiCache](https://img.shields.io/badge/AWS%20ElastiCache-C925D1?style=for-the-badge&logo=amazon-aws&logoColor=white) | 관리형 Redis |
| ![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white) | 파일 스토리지 |
| ![AWS ALB](https://img.shields.io/badge/AWS%20ALB-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white) | 로드 밸런서 |
| AWS Secrets Manager | 환경 변수 및 비밀 관리 |
| AWS OpenSearch | Elasticsearch 관리형 서비스 (선택사항) |

### 🐳 Container & Orchestration

| 기술 | 용도 |
|------|------|
| ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) | 컨테이너화 |
| ![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white) | 로컬 멀티 컨테이너 환경 |

### 🌐 API Gateway & Proxy

| 기술 | 용도 |
|------|------|
| ![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white) | 리버스 프록시 & API Gateway |

### 🔄 CI/CD

| 기술 | 용도 |
|------|------|
| ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white) | 자동화된 빌드, 테스트, 배포 |

### 📊 Monitoring & Logging

| 기술 | 용도 |
|------|------|
| ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white) | 메트릭 수집 및 저장 |
| ![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white) | 메트릭 시각화 대시보드 |
| Spring Actuator | 애플리케이션 모니터링 엔드포인트 |
| Micrometer | 메트릭 수집 라이브러리 |

### 🧪 Testing & Code Quality

| 기술 | 용도 |
|------|------|
| JUnit 5 | 단위 테스트 프레임워크 |
| ![SonarQube](https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white) | 정적 코드 분석 |
| k6 | 부하 테스트 도구 |
| Bean Validation | 입력 데이터 검증 |

### 🔧 Utilities & Libraries

| 라이브러리 | 용도 |
|-----------|------|
| MapStruct | Entity ↔ DTO 자동 매핑 |
| Resilience4j | Circuit Breaker, Rate Limiter |
| Jackson | JSON 직렬화/역직렬화 |
| Apache Commons Lang3 | 유틸리티 함수 |
| Lombok | 보일러플레이트 코드 감소 |
| Springdoc OpenAPI | API 문서 자동 생성 (Swagger UI) |

---

## 🏗️ 아키텍처 특징

### 멀티모듈 구조
```
mopl/
├── core/            # 공통 계약 (이벤트 DTO, ErrorCode)
├── api/             # REST API 서버
├── socket/          # WebSocket/SSE 실시간 서버
├── worker/          # Kafka 이벤트 소비 & 비동기 처리
├── batch/           # Spring Batch 스케줄링 작업
├── gateway/         # Nginx 리버스 프록시
└── monitoring/      # Prometheus & Grafana
```

### 이벤트 기반 아키텍처
- **Kafka**를 통한 모듈 간 느슨한 결합
- 비동기 작업 처리 (알림, 집계, 통계)
- 이벤트 소싱 패턴 적용

### 확장 가능한 설계
- **Redis**를 활용한 세션 클러스터링
- **AWS ECS Fargate**로 컨테이너 자동 확장
- 멀티 인스턴스 배포 지원

### 보안
- JWT 토큰 기반 무상태 인증
- OAuth2 소셜 로그인 (Google, Kakao)
- CSRF 토큰을 통한 공격 방어
- Resilience4j Rate Limiter로 DDoS 방어

---
---
## 시스템 아키텍처
