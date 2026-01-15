USE mopl;

-- admin 사용자가 참여 중인 대화방 중 하나 선택 (가장 최근 것)
SELECT conversation_id INTO @target_conversation_id
FROM conversation_participants
WHERE user_id = '019b72f1-dd7b-75e7-a15c-05b18292ffe7' -- admin ID
ORDER BY joined_at DESC
LIMIT 1;

-- 상대방 ID 찾기
SELECT user_id INTO @partner_id
FROM conversation_participants
WHERE conversation_id = @target_conversation_id
  AND user_id != '019b72f1-dd7b-75e7-a15c-05b18292ffe7'
LIMIT 1;

-- 멱등성 보장: 기존에 생성된 테스트용 메시지만 삭제
DELETE FROM direct_messages 
WHERE conversation_id = @target_conversation_id 
  AND content LIKE 'Pagination Test Message %';

-- 메시지 100개 삽입 (2분 간격)
INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content, created_at, read_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100
)
SELECT
    UUID(),
    @target_conversation_id,
    CASE WHEN n % 2 = 1 THEN '019b72f1-dd7b-75e7-a15c-05b18292ffe7' ELSE @partner_id END,
    CASE WHEN n % 2 = 1 THEN @partner_id ELSE '019b72f1-dd7b-75e7-a15c-05b18292ffe7' END,
    CONCAT('Pagination Test Message ', LPAD(n, 3, '0')),
    NOW(6) - INTERVAL (200 - (n * 2)) MINUTE, -- 2분 간격 (최대 200분 전부터 현재까지)
    CASE WHEN n < 80 THEN NOW(6) ELSE NULL END
FROM seq;

SELECT CONCAT('Successfully re-inserted 100 messages into conversation: ', @target_conversation_id) AS result;