USE `mopl`;
SET NAMES 'utf8mb4'; -- 한글/특수문자 깨짐 방지

-- ==============================
-- 대화 검색/페이지네이션 테스트 데이터
-- - 로컬용
-- - admin 계정 기준 대화 50개, 전체 대화 200개
-- - DM 약 2,000건(대화당 10건)
-- - 키워드 분포: "ㅇ", "테스트", "안녕", "hello", "search", "alpha", "beta"
-- ==============================

-- 한글 리터럴(UTF-8 바이트 직접 지정)
SET @kw_yo = CONVERT(_utf8mb4 0xE38587 USING utf8mb4); -- "ㅇ"
SET @kw_test = CONVERT(_utf8mb4 0xED858CEC8AA4ED8AB8 USING utf8mb4); -- "테스트"
SET @kw_test_user = CONVERT(_utf8mb4 0xED858CEC8AA4ED8AB8EC9CA0ECA080 USING utf8mb4); -- "테스트유저"
SET @kw_hello_kr = CONVERT(_utf8mb4 0xEC9588EB8595 USING utf8mb4); -- "안녕"
SET @kw_user = CONVERT(_utf8mb4 0xEC82ACEC9AA9EC9E90 USING utf8mb4); -- "사용자"
SET @kw_test_message = CONVERT(_utf8mb4 0xED858CEC8AA4ED8AB820EBA994EC8B9CECA780 USING utf8mb4); -- "테스트 메시지"

-- 깨진 UUID(바이너리) 데이터 정리: CHAR(36) 컬럼에 비정상 값이 들어간 경우 제거
DELETE FROM direct_messages
WHERE id NOT REGEXP '^[0-9a-fA-F-]{36}$'
   OR conversation_id NOT REGEXP '^[0-9a-fA-F-]{36}$'
   OR sender_id NOT REGEXP '^[0-9a-fA-F-]{36}$'
   OR receiver_id NOT REGEXP '^[0-9a-fA-F-]{36}$';

-- 멱등 처리: 이전 테스트 데이터 정리
DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_conversations;
CREATE TEMPORARY TABLE tmp_cleanup_conversations AS
SELECT DISTINCT cp.conversation_id AS conversation_id
FROM conversation_participants cp
JOIN users u ON u.id = cp.user_id
WHERE u.email LIKE 'search_user_%';

DELETE FROM direct_messages
WHERE conversation_id IN (SELECT conversation_id FROM tmp_cleanup_conversations);

DELETE FROM conversation_participants
WHERE conversation_id IN (SELECT conversation_id FROM tmp_cleanup_conversations);

DELETE FROM conversations
WHERE id IN (SELECT conversation_id FROM tmp_cleanup_conversations);

DELETE FROM users
WHERE email LIKE 'search_user_%';

-- 0) 숫자 시퀀스 (1~200)
DROP TEMPORARY TABLE IF EXISTS tmp_numbers;
CREATE TEMPORARY TABLE tmp_numbers (n INT NOT NULL PRIMARY KEY);
INSERT INTO tmp_numbers (n)
SELECT a.n + b.n * 10 + c.n * 100 + 1 AS n
FROM (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) a
CROSS JOIN (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) b
CROSS JOIN (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) c
WHERE a.n + b.n * 10 + c.n * 100 + 1 <= 200;

-- 1) 검색용 사용자 60명 생성 (email prefix로 구분)
INSERT INTO users (id, email, name, password_hash, auth_provider, provider_user_id, role, locked, profile_image_url, created_at, updated_at)
SELECT
  CAST(LOWER(UUID()) AS CHAR(36)),
  CONCAT('search_user_', LPAD(n, 3, '0'), '@mopl.com'),
  CASE
    WHEN n % 10 = 1 THEN CONCAT(@kw_yo, @kw_test, LPAD(n, 2, '0'))
    WHEN n % 10 = 2 THEN CONCAT(@kw_test_user, LPAD(n, 2, '0'))
    WHEN n % 10 = 3 THEN CONCAT(@kw_hello_kr, LPAD(n, 2, '0'))
    WHEN n % 10 = 4 THEN CONCAT('hello', LPAD(n, 2, '0'))
    WHEN n % 10 = 5 THEN CONCAT('alpha', LPAD(n, 2, '0'))
    WHEN n % 10 = 6 THEN CONCAT('beta', LPAD(n, 2, '0'))
    ELSE CONCAT(@kw_user, LPAD(n, 2, '0'))
  END,
  CONCAT('hash_search_', LPAD(n, 3, '0')),
  'LOCAL',
  NULL,
  'USER',
  0,
  NULL,
  NOW(6) - INTERVAL (1000 + n) SECOND,
  NOW(6) - INTERVAL (1000 + n) SECOND
FROM tmp_numbers
WHERE n <= 60;

DROP TEMPORARY TABLE IF EXISTS tmp_search_users;
CREATE TEMPORARY TABLE tmp_search_users (
  rn INT NOT NULL,
  id CHAR(36) NOT NULL
);

INSERT INTO tmp_search_users (rn, id)
SELECT
  (@rn := @rn + 1) AS rn,
  u.id
FROM users u
CROSS JOIN (SELECT @rn := 0) r
WHERE email LIKE 'search_user_%'
ORDER BY email;

DROP TEMPORARY TABLE IF EXISTS tmp_search_users1;
DROP TEMPORARY TABLE IF EXISTS tmp_search_users2;
CREATE TEMPORARY TABLE tmp_search_users1 AS
SELECT rn, id FROM tmp_search_users;
CREATE TEMPORARY TABLE tmp_search_users2 AS
SELECT rn, id FROM tmp_search_users;

-- 2) 대화 200개 생성 (admin 50개 포함)
DROP TEMPORARY TABLE IF EXISTS tmp_conversations;
CREATE TEMPORARY TABLE tmp_conversations (
  id CHAR(36) NOT NULL,
  user1 CHAR(36) NOT NULL,
  user2 CHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  seq INT NOT NULL
);

-- 2-1) admin 계정 기준 대화 50개
INSERT INTO tmp_conversations (id, user1, user2, created_at, seq)
SELECT
  CAST(LOWER(UUID()) AS CHAR(36)),
  '019b72f1-dd7b-75e7-a15c-05b18292ffe7',
  su.id,
  NOW(6) - INTERVAL n MINUTE,
  n
FROM tmp_numbers
JOIN tmp_search_users su ON su.rn = n
WHERE n <= 50;

-- 2-2) 나머지 대화 150개 (일반 사용자끼리)
INSERT INTO tmp_conversations (id, user1, user2, created_at, seq)
SELECT
  CAST(LOWER(UUID()) AS CHAR(36)),
  su1.id,
  su2.id,
  NOW(6) - INTERVAL n MINUTE,
  n
FROM tmp_numbers
JOIN tmp_search_users1 su1 ON su1.rn = ((n - 1) % 60) + 1
JOIN tmp_search_users2 su2 ON su2.rn = (n % 60) + 1
WHERE n BETWEEN 51 AND 200;

INSERT INTO conversations (id, created_at, updated_at)
SELECT id, created_at, created_at FROM tmp_conversations;

-- 3) 참여자 데이터 생성 (읽음 시간은 일부 NULL로 섞음)
INSERT INTO conversation_participants (conversation_id, user_id, joined_at, last_read_at)
SELECT
  t.id,
  t.user1,
  t.created_at,
  CASE
    WHEN t.seq % 3 = 0 THEN NULL
    ELSE t.created_at + INTERVAL 2 MINUTE
  END
FROM tmp_conversations t;

INSERT INTO conversation_participants (conversation_id, user_id, joined_at, last_read_at)
SELECT
  t.id,
  t.user2,
  t.created_at,
  CASE
    WHEN t.seq % 4 = 0 THEN NULL
    ELSE t.created_at + INTERVAL 3 MINUTE
  END
FROM tmp_conversations t;

-- 4) DM 생성: 대화당 10건 (총 2,000건)
INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content, created_at, read_at)
SELECT
  CAST(LOWER(UUID()) AS CHAR(36)),
  t.id,
  CASE WHEN msg_seq.n % 2 = 1 THEN t.user1 ELSE t.user2 END,
  CASE WHEN msg_seq.n % 2 = 1 THEN t.user2 ELSE t.user1 END,
  CASE
    WHEN msg_seq.n % 7 = 1 THEN CONCAT(@kw_test_message, ' ', msg_seq.n)
    WHEN msg_seq.n % 7 = 2 THEN CONCAT(@kw_hello_kr, ' ', msg_seq.n)
    WHEN msg_seq.n % 7 = 3 THEN CONCAT('hello ', msg_seq.n)
    WHEN msg_seq.n % 7 = 4 THEN CONCAT('search ', msg_seq.n)
    WHEN msg_seq.n % 7 = 5 THEN CONCAT('alpha ', msg_seq.n)
    WHEN msg_seq.n % 7 = 6 THEN CONCAT('beta ', msg_seq.n)
    ELSE CONCAT(@kw_yo, ' ', msg_seq.n)
  END,
  t.created_at + INTERVAL msg_seq.n MINUTE,
  CASE
    WHEN msg_seq.n % 4 = 0 THEN NULL
    ELSE t.created_at + INTERVAL (msg_seq.n + 1) MINUTE
  END
FROM tmp_conversations t
JOIN tmp_numbers msg_seq ON msg_seq.n <= 10;

-- 참고: 키워드 검색 테스트
-- - 이름 키워드: ㅇ, 테스트, 안녕, hello, alpha, beta
-- - DM 키워드: ㅇ, 테스트, 안녕, hello, search, alpha, beta
