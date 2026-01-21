USE `mopl`;
SET NAMES 'utf8mb4';

-- ============================================
-- 페이지네이션 테스트를 위한 리뷰 데이터 추가
-- 브레이킹 배드 콘텐츠에 88개 리뷰 추가 (기존 12개 + 88개 = 총 100개)
-- ============================================

-- 임시 테이블로 테스트 사용자 생성
DROP TEMPORARY TABLE IF EXISTS temp_test_users;
CREATE TEMPORARY TABLE temp_test_users (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(50),
    row_num INT
);

-- 88명의 테스트 사용자 데이터 생성
INSERT INTO temp_test_users (id, name, row_num)
SELECT
    UUID() as id,
    CONCAT('페이지테스터', LPAD(n, 3, '0')) as name,
    n as row_num
FROM (
         SELECT t1.n + t2.n * 10 + t3.n * 100 + 1 as n
         FROM
             (SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
              UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
             (SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
              UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
             (SELECT 0 as n UNION SELECT 1) t3
             LIMIT 88
     ) numbers;

-- users 테이블에 테스트 사용자 추가
INSERT INTO users (id, email, name, password_hash, auth_provider, provider_user_id, role, locked, profile_image_key, created_at, updated_at)
SELECT
    id,
    CONCAT('pagination_test_', LPAD(row_num, 3, '0'), '@test.com'),
    name,
    'hash_test_pagination',
    'LOCAL',
    NULL,
    'USER',
    0,
    NULL,
    NOW(6),
    NOW(6)
FROM temp_test_users;

-- 리뷰 텍스트 샘플
DROP TEMPORARY TABLE IF EXISTS temp_review_texts;
CREATE TEMPORARY TABLE temp_review_texts (
    text_content VARCHAR(200),
    row_num INT AUTO_INCREMENT PRIMARY KEY
);

INSERT INTO temp_review_texts (text_content) VALUES
                                                 ('정말 재미있게 봤습니다. 강추!'),
                                                 ('시간 가는 줄 몰랐네요.'),
                                                 ('기대 이상이었어요.'),
                                                 ('몰입감이 정말 좋았습니다.'),
                                                 ('명작이라는 평가가 이해가 갑니다.'),
                                                 ('적당히 볼 만했어요.'),
                                                 ('그냥 저냥 평범했습니다.'),
                                                 ('시간 때우기 좋네요.'),
                                                 ('나쁘지 않았어요.'),
                                                 ('다시 보고 싶은 작품입니다.'),
                                                 ('주변에 추천하고 싶어요.'),
                                                 ('스토리 전개가 탄탄해요.'),
                                                 ('연기가 인상 깊었습니다.'),
                                                 ('여운이 남는 작품이네요.'),
                                                 ('생각보다 괜찮았어요.'),
                                                 ('기대했던 것보다 아쉬웠습니다.'),
                                                 ('다음 편이 기대되네요.'),
                                                 ('한번쯤 볼 만해요.'),
                                                 ('인생 작품 추가했습니다.'),
                                                 ('감동적이었어요.');

-- 브레이킹 배드에 88개 리뷰 추가
INSERT INTO reviews (id, content_id, author_id, text, rating, created_at, updated_at)
SELECT
    UUID() as id,
    'a1e21d98-bc59-4682-a78d-cd556457f482' as content_id,
    u.id as author_id,
    t.text_content as text,
    ROUND(1.0 + (RAND() * 4.0), 1) as rating,
    DATE_SUB(NOW(6), INTERVAL (88 - u.row_num) MINUTE) as created_at,
    DATE_SUB(NOW(6), INTERVAL (88 - u.row_num) MINUTE) as updated_at

FROM temp_test_users u
         CROSS JOIN temp_review_texts t
WHERE u.row_num <= 88
  AND t.row_num = ((u.row_num - 1) % 20) + 1
ORDER BY u.row_num
    LIMIT 88;

-- 임시 테이블 정리
DROP TEMPORARY TABLE IF EXISTS temp_test_users;
DROP TEMPORARY TABLE IF EXISTS temp_review_texts;

-- 확인용 쿼리
SELECT
    c.title as '콘텐츠',
    COUNT(r.id) as '총 리뷰 수',
    ROUND(AVG(r.rating), 2) as '평균 평점'
FROM contents c
         LEFT JOIN reviews r ON c.id = r.content_id
WHERE c.id = 'a1e21d98-bc59-4682-a78d-cd556457f482'
GROUP BY c.id, c.title;

-- ============================================
-- 테스트 완료 후 데이터 삭제 방법
-- ============================================
-- 아래 쿼리들은 필요시 주석 해제하여 사용

-- 1. 테스트 리뷰만 삭제
-- DELETE FROM reviews
-- WHERE author_id IN (
--     SELECT id FROM users WHERE email LIKE 'pagination_test_%@test.com'
-- );

-- 2. 테스트 사용자도 함께 삭제
-- DELETE FROM users WHERE email LIKE 'pagination_test_%@test.com';

-- 3. 또는 한번에 삭제 (외래키 제약조건 주의)
-- DELETE u, r
-- FROM users u
-- LEFT JOIN reviews r ON u.id = r.author_id
-- WHERE u.email LIKE 'pagination_test_%@test.com';