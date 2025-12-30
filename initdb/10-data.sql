USE `mopl`;
SET NAMES 'utf8mb4'; -- 한글/특수문자 깨짐 방지

-- users dummy data
INSERT INTO users (id, email, name, password_hash, auth_provider, provider_user_id, role, locked, profile_image_url, created_at, updated_at)
VALUES
-- 1. 관리자 계정
('a1111111-1111-1111-1111-111111111111', 'admin@mopl.com', '관리자', 'hash_admin_secret', 'LOCAL', NULL, 'ADMIN', 0, 'https://example.com/profiles/admin.png', DEFAULT, DEFAULT),

-- 2. 일반 사용자 (로컬 가입)
('a2222222-2222-2222-2222-222222222222', 'chulsu@naver.com', '김철수', 'hash_chulsu_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),
('a3333333-3333-3333-3333-333333333333', 'younghee@gmail.com', '이영희', 'hash_younghee_pwd', 'LOCAL', NULL, 'USER', 0, 'https://example.com/profiles/yh.png', DEFAULT, DEFAULT),
-- 2.2 리뷰 채우기용으로 일반 사용자 6명 추가
('a7777777-7777-7777-7777-777777777777', 'test1@test.com', '홍길동', 'hash_test1_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),
('a8888888-8888-8888-8888-888888888888', 'test2@test.com', '김길동', 'hash_test2_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),
('a9999999-9999-9999-9999-999999999999', 'test3@test.com', '박길동', 'hash_test3_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),
('a1010101-0101-0101-0101-010101010101', 'test4@test.com', '박예나', 'hash_test4_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),
('a1212121-2121-2121-2121-212121212121', 'test5@test.com', '김예나', 'hash_test5_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),
('a1313131-3131-3131-3131-313131313131', 'test6@test.com', '이예나', 'hash_test6_pwd', 'LOCAL', NULL, 'USER', 0, NULL, DEFAULT, DEFAULT),

-- 3. 외부 제공자(소셜) 가입 사용자
('a4444444-4444-4444-4444-444444444444', 'kakao_tester@kakao.com', '박카카오', 'social_no_pwd', 'KAKAO', 'kakao_id_12345', 'USER', 0, NULL, DEFAULT, DEFAULT),
('a5555555-5555-5555-5555-555555555555', 'google_tester@google.com', '구글러', 'social_no_pwd', 'GOOGLE', 'google_id_67890', 'USER', 0, NULL, DEFAULT, DEFAULT),

-- 4. 잠긴 계정 (locked = 1)
('a6666666-6666-6666-6666-666666666666', 'locked_user@mopl.com', '정차단', 'hash_locked_pwd', 'LOCAL', NULL, 'USER', 1, NULL, DEFAULT, DEFAULT);



-- tags dummy data
INSERT INTO tags(id, name)
VALUES
    ('b1111111-1111-1111-1111-111111111111', '액션'),
    ('b2222222-2222-2222-2222-222222222222', '드라마'),
    ('b3333333-3333-3333-3333-333333333333', '범죄'),
    ('b4444444-4444-4444-4444-444444444444', '스릴러'),
    ('b5555555-5555-5555-5555-555555555555', '스포츠'),
    ('b6666666-6666-6666-6666-666666666666', 'SF'),
    ('b7777777-7777-7777-7777-777711111111', '애니메이션'),
    ('b8888888-8888-8888-8888-888811111111', '코미디');


-- content dummy data
INSERT INTO contents (id, type, external_id, title, description, thumbnail_url, average_rating, review_count, watcher_count, created_at, updated_at) VALUES
('a1e21d98-bc59-4682-a78d-cd556457f482', 'tvSeries', '1396', '브레이킹 배드',
'2008년 1월 AMC에서 방영을 시작한 범죄 스릴러. Breaking Bad는 막가기를 뜻하는 미국 남부 지방의 속어이다. 한때 노벨화학상까지 바라 볼 정도로 뛰어난 과학자였던 고등학교 화학 교사 월터 화이트는 자신의 50세 생일 날에 폐암 3기 진단을 받는다. 어느 날 동서와 함께 마약 단속 현장을 참관한 그는 현장에서 달아나는 옛 제자 제시를 발견한다. 뇌성마비에 걸린 고등학생 아들과 임신한 아내를 위해 제시에게 동업을 제의한 월터는 자신의 화학지식을 이용해 전례없는 고순도 고품질의 메스암페타민을 제조한다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/89a1c53b-88e0-469d-9a8b-4d3d2f947fcc?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=2dc4e83542491e52eb075477ceed14d20725dda8e8aaefecca66413b83d57d27',
3.73, 12, 1, DEFAULT, DEFAULT),
('011150c5-4ddf-4615-9342-3e2ee925a319', 'movie', '865', '더 러닝 맨',
'실직한 가장 ‘벤 리처즈’가 거액의 상금을 위해 30일간 잔인한 추격자들로부터 살아남아야 하는 글로벌 서바이벌 프로그램에 참가하며 펼쳐지는 추격 액션 블록버스터',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/0f1500d6-509b-4a98-a1e2-958b42afa1cd?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=de396eb8863d47a45acf1ddfa2ee8527c8d7727f2e1c9f8f6e7bab041cffc34e',
0.00, 0, 0, DEFAULT, DEFAULT),
('0b57b34c-9c62-44b2-8837-c2e215fd6718', 'sport', 's101', 'Aston Villa vs Bournemouth',
'English Premier League 2025-11-09 Aston Villa vs Bournemouth',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/38c4fe19-0133-4ec6-8107-98266ed776b9?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=1f20a61d230271ca888bf7d477379f1f3688419e6ef007a0a4f7f2d7b40f7df0',
3.10, 2, 0, DEFAULT, DEFAULT),
('aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'tvSeries', '66732', '기묘한 이야기',
'인디애나주의 작은 마을에서 행방불명된 소년. 이와 함께 미스터리한 힘을 가진 소녀가 나타나고, 마을에는 기묘한 현상들이 일어나기 시작한다. 아들을 찾으려는 엄마와 마을 사람들은 이제 정부의 일급비밀 실험의 실체와 무시무시한 기묘한 현상들에 맞서야 한다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/56ad6e97-cce8-4afd-8bf3-df72a13e37e2?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=39b43ef7ed05577842510bd8da1ec483559d2009284af04d633121fc8fd75045',
4.07, 6, 0, DEFAULT, DEFAULT),
('01fd8ad2-b219-4480-be07-6d9030bf9b95', 'tvSeries', '215286', 'คลั่ง/รัก/นักโทษ',
'',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/49fa7b09-0112-47c7-8d3f-03e92611a030?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=9f2ff417d939c8d009f23fece6e8b35324b07de5ba9a816947046810062aa470',
0.00, 0, 0, DEFAULT, DEFAULT),
('32cf12ee-6e15-41de-941e-5eb6c9ca621e', 'movie', '1183181', '힘',
'미식축구 리그 진출을 앞둔 유망한 쿼터백 ‘캐머런 케이드’. 컴바인을 준비하던 어느 날, 정체불명의 인물에게 습격을 당하고 치명적인 부상을 입는다. 선수로서의 생명이 불투명해진 그에게 어릴 적 자신이 우상으로 여겼던 전설적인 쿼터백 ‘아이제이아 화이트’가 나타나 함께 훈련하자는 제안을 한다. 역대 최고라 불리는 선수와 함께 한다는 기쁨도 잠시 ‘최고에는 희생이 따른다’는 슬로건 아래 기이한 방식의 훈련이 이어지고 이로 인해 정신적인 혼란까지 겪게 된 그는 끝내 어두운 이면을 마주한다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/bdc54209-ed95-4cbd-aeac-b7816bf064f8?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=5cde1640a5e5b3ac8ef6f8e1cf786238b4b910d688945ead982e483730683101',
0.00, 0, 0, DEFAULT, DEFAULT),
('013a3fff-145f-4fdd-bf4d-b87f47af7262', 'tvSeries', '258838', '''라스트 프런티어'' - The Last Frontier',
'죄수 수송 비행기가 외진 알래스카 황무지에 추락하며 난폭한 죄수들이 풀려나자, 그 지역의 유일한 경찰은 자기가 지키기로 맹세한 마을의 안전을 위해 애쓴다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/3738d9a6-539c-4244-ad66-e8f2c4ff970b?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=579eaed6e7782665f4fb1c5982b6255700d3852313c95ebd3408f5d3b6d385fd',
0.00, 0, 0, DEFAULT, DEFAULT),
('018b512c-4d0f-4e65-bd01-272ee8e4c293', 'tvSeries', '234220', '아이 러브 LA',
'',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/feae8fb7-c9dd-43bf-9ad8-f08f833db73f?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=6eeca1ba4701e8cc415fd459f8b793ef6918013c5b236b7625350156a4f8577a',
0.00, 0, 0, DEFAULT, DEFAULT),
('039561b6-523e-47ba-8231-d8117f2a63ed', 'tvSeries', '241512', '복권에 당첨되는 법',
'경제적으로 절박해진 평범한 남자. 아웃사이더들을 모아, 생방송 TV 쇼에서 복권 당첨금을 훔치는 대담한 범행을 기획한다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/6c8422aa-d5ce-4221-8c9a-d8ee7b3f632f?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=0f159e0e18b857e6f2953bca66f79eb46a7641bb3a837d22e028153c3c597709',
0.00, 0, 0, DEFAULT, DEFAULT),
('043d349a-248e-457d-8aac-59dd1a1e51b9', 'sport', 's102', 'Getafe vs Atlético Madrid',
'Spanish La Liga 2025-11-23 Getafe vs Atlético Madrid',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/ab77c374-1d62-49b0-b4d7-9f90e1cea5be?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=c6b4fd0776fb6431cfc0821b2d5c1da80e73013fc36b6ee0fc41b8804931fffd',
0.00, 0, 0, DEFAULT, DEFAULT),
('0462ffb5-e98a-4c12-8f30-4c052c63bcb9', 'movie', '1097848', '굿 포춘',
'천사가 건드린 인생 역전, 운명의 저울이 흔들린다. 선의는 넘치지만 영 서툰 천사 가브리엘이 삶의 끝자락에서 버티는 긱 워커 ‘아지’와 호사 속을 굴리는 벤처 자본가 ‘제프’의 일상에 개입한다. 작은 도움으로 시작된 일은 두 사람의 세계를 기묘하게 뒤섞고, 돈과 성공이 진짜 행복을 보장하지 않음을 증명하려던 가브리엘의 계획마저 엉키기 시작한다. 사랑과 우정, 자존감 사이에서 균형을 찾으려 애쓰는 이들은 각자 놓쳐온 것을 돌아보게 되고, 결국 ‘행운’의 의미를 다시 정의하게 되는데...',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/2750d8bd-69e7-467d-81cd-fe708ef93bd4?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=4a8c47b231d0faf6bd5bbf5454c531199ebc13f473734d0a2868dd60a9be61c3',
0.00, 0, 0, DEFAULT, DEFAULT),
('05aa5825-2398-4d12-8609-8e9c8c95460e', 'sport', 's103', 'Real Betis vs Girona',
'Spanish La Liga 2025-11-23 Real Betis vs Girona',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/291a3199-daeb-4685-8a17-eeee00b5358d?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=0b0846ff0197e5b097b7fd2a36c4b16566737630d2649288d3b009f5c4dc9ea9',
0.00, 0, 0, DEFAULT, DEFAULT),
('05ba0c76-5623-400a-b198-30308624d32a', 'movie', '269149', '주토피아',
'어릴 적부터 경찰이 꿈이었던 토끼 주디 홉스는 주변의 만류에도 불구하고 경찰학교에 들어가 당당히 수석으로 졸업한다. 온갖 동물들이 모여 살며 교양 있고 세련된 라이프 스타일을 주도하는 대도시 주토피아에 자원한 주디는 의욕을 안고 출근하지만, 상사는 작은 토끼라는 이유로 주차관리 같은 소일거리만 시킨다. 따분하게 업무를 보던 주디는 아이스크림 불법 판매를 일삼는 사기꾼 여우 닉 와일드를 알게 되고, 그와 함께 48시간 안에 주토피아에서 벌어지고 있는 연쇄 실종사건을 추적해야만 하는데...',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/5fc9fd01-f694-4980-b763-ecbe2bf55c75?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=59ce0e842c7997c7a6030943cc1b6ea9ea1d8eca17bf138a493435b52edd33c5',
0.00, 0, 0, DEFAULT, DEFAULT),
('064e73f8-2646-4eb9-9781-af133a173a1a', 'sport', 's104', 'Mallorca vs Girona',
'Spanish La Liga 2026-01-04 Mallorca vs Girona',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/8e1bea22-df6a-4580-91f9-c6ddc3ed0b3a?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=6742eb27cc733ca4d1eb389cc1544db8fbcb3041e8e3db64f9f665dd813584be',
0.00, 0, 0, DEFAULT, DEFAULT),
('06d82ece-767d-4197-9529-ddb24263bd60', 'sport', 's105', 'Arsenal vs Wolverhampton Wanderers',
'English Premier League 2025-12-13 Arsenal vs Wolverhampton Wanderers',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/ed79c308-ec5e-4f94-865e-67e1fed74cbb?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=896444f95adf268b229215982d6d6c5361eaabca1971078f7bceb43d5afe3aea',
0.00, 0, 0, DEFAULT, DEFAULT),
('07588c12-1192-4ef8-b6c8-f488ba8bcb96', 'sport', 's106', 'Barcelona vs Athletic Bilbao',
'Spanish La Liga 2025-11-22 Barcelona vs Athletic Bilbao',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/f552bca5-61d5-4b28-9574-fb0bbd561b53?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=1746d5c9452b1fdb258d64293a10b7acc3215748430e1e696fb1213ea728563e',
0.00, 0, 0, DEFAULT, DEFAULT),
('08aa93a9-35dd-4aed-9f53-b9f6b5b33daa', 'movie', '1241982', '극장판 귀멸의 칼날: 무한성편',
'혈귀로 변해버린 여동생 네즈코를 인간으로 되돌리기 위해 혈귀를 사냥하는 조직인 《귀살대》에 입대한 카마도 탄지로. 입대 후 동료인 아가츠마 젠이츠, 하시비라 이노스케와 함께 많은 혈귀와 싸우고, 성장하면서 세 사람의 우정과 유대는 깊어진다. 탄지로는 《귀살대》 최고위 검사인 《주》와도 함께 싸웠다. 「무한열차」에서는 염주・렌고쿠 쿄쥬로, 「유곽」에서는 음주・우즈이 텐겐, 「도공 마을」에서는 하주・토키토 무이치로, 연주・칸로지 미츠리와 함께 혈귀를 상대로 격렬한 전투를 벌였다. 그 후 다가올 혈귀와의 결전에 대비해 귀살대원들과 함께 《주》가 주도하는 합동 강화 훈련에 참가해 훈련을 받던 도중 《귀살대》의 본부인 우부야시키 저택에 나타난 키부츠지 무잔. 어르신의 위기에 달려온 《주》들과 탄지로였지만, 무잔의 술수로 의문의 공간으로 떨어지고 말았는데. 탄지로 일행이 떨어진 곳, 그곳은 혈귀의 본거지 《무한성》─ “귀살대”와 “혈귀”의 최종 결전의 포문이 열린다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/c7f79237-ad7e-4715-b63a-a02f5448d430?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=c9850f8308eb4f9ec246c4df078071b89f1314f1dd544cf687f0cc861f8b1c3b',
0.00, 0, 0, DEFAULT, DEFAULT),
('0a4b929d-b987-45fc-b6ea-e06730847c8b', 'tvSeries', '103786', '종말의 발키리',
'7백만 년 인류 역사에 종지부를 찍기로 한 신들. 그들이 인간에게 마지막 기회를 준다. 전 세계의 신과 사상 최강의 인간이 일대일로 맞붙는 열세 번의 승부, ‘라그나로크’가 지금 시작된다!',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/664108a5-758d-4185-88f8-445e075bf959?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=6d2ba2f9e6a4e46723d76dbf0ffabe968c7486d0b61619e2f2c73344de6d1f2c',
0.00, 0, 0, DEFAULT, DEFAULT),
('0ba299f3-0ce7-4d08-9c7a-f9e2a3ea8cdc', 'tvSeries', '135934', '唐诡奇谭',
'',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/ac5617a3-b7ac-4429-92bf-5e707685da8b?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=55a2d98844546c5816c30380939ce9f83a42a2a3b0f26e8dd3db4166cf3589de',
0.00, 0, 0, DEFAULT, DEFAULT),
('0c279d74-ce72-4272-b05b-5a5bc22c2f41', 'tvSeries', '280695', '''플루리부스: 행복의 시대'' - Pluribus',
'세상에서 가장 불행한 사람이 세상을 ''행복''으로부터 구해야 한다.',
'https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/mopl/images/1c7d9c21-0e74-49cf-82bb-dbb374f9df80?response-content-type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20251222T063243Z&X-Amz-SignedHeaders=host&X-Amz-Credential=AKIAQ67VTHC26F6IWSAU%2F20251222%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=d7767cfe370a28c868f9ab2af6a5e7abf3d0839179da33a919d3825a12d324ce',
0.00, 0, 0, DEFAULT, DEFAULT);


-- content_tags dummy data
INSERT INTO content_tags (content_id, tag_id)
VALUES
-- 1. 브레이킹 배드 (a1e21...) : 범죄, 드라마, 스릴러 태그 연결
('a1e21d98-bc59-4682-a78d-cd556457f482', 'b3333333-3333-3333-3333-333333333333'), -- 범죄
('a1e21d98-bc59-4682-a78d-cd556457f482', 'b2222222-2222-2222-2222-222222222222'), -- 드라마
('a1e21d98-bc59-4682-a78d-cd556457f482', 'b4444444-4444-4444-4444-444444444444'), -- 스릴러

-- 2. 더 러닝 맨 (01115...) : 액션, SF 태그 연결
('011150c5-4ddf-4615-9342-3e2ee925a319', 'b1111111-1111-1111-1111-111111111111'), -- 액션
('011150c5-4ddf-4615-9342-3e2ee925a319', 'b6666666-6666-6666-6666-666666666666'), -- SF

-- 3. 기묘한 이야기 (aa807...) : 드라마, 스릴러, SF 태그 연결
('aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'b2222222-2222-2222-2222-222222222222'), -- 드라마
('aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'b4444444-4444-4444-4444-444444444444'), -- 스릴러
('aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'b6666666-6666-6666-6666-666666666666'), -- SF

-- 4. Aston Villa 경기 (0b57b...) : 스포츠 태그 연결
('0b57b34c-9c62-44b2-8837-c2e215fd6718', 'b5555555-5555-5555-5555-555555555555'); -- 스포츠

-- playlists dummy data
INSERT INTO playlists (id, owner_id, title, description, subscriber_count, created_at, updated_at)
VALUES
-- 1. 김철수(u2222...)의 플레이리스트
('c1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222',
 '주말에 정주행할 드라마', '이번 주말에 몰아볼 명작 드라마 모음입니다.', 5, DEFAULT, DEFAULT),

-- 2. 이영희(u3333...)의 플레이리스트
('c2222222-2222-2222-2222-222222222222', 'a3333333-3333-3333-3333-333333333333',
 '최신 개봉 영화 기대작', '보고 싶은 영화들을 정리해두었습니다.', 12, DEFAULT, DEFAULT),

-- 3. 박카카오(u4444...)의 플레이리스트
('c3333333-3333-3333-3333-333333333333', 'a4444444-4444-4444-4444-444444444444',
 '스포츠 하이라이트', '박진감 넘치는 경기들만 모았습니다.', 0, DEFAULT, DEFAULT);


-- playlist_contents dummy data
INSERT INTO playlist_contents (playlist_id, content_id, added_at)
VALUES
-- 1. '주말에 정주행할 드라마' 플레이리스트 (p1111...)
-- 담긴 내용: 브레이킹 배드, 기묘한 이야기
('c1111111-1111-1111-1111-111111111111', 'a1e21d98-bc59-4682-a78d-cd556457f482', DEFAULT),
('c1111111-1111-1111-1111-111111111111', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', DEFAULT),

-- 2. '최신 개봉 영화 기대작' 플레이리스트 (p2222...)
-- 담긴 내용: 더 러닝 맨, 힘
('c2222222-2222-2222-2222-222222222222', '011150c5-4ddf-4615-9342-3e2ee925a319', DEFAULT),
('c2222222-2222-2222-2222-222222222222', '32cf12ee-6e15-41de-941e-5eb6c9ca621e', DEFAULT),

-- 3. '스포츠 하이라이트' 플레이리스트 (p3333...)
-- 담긴 내용: Aston Villa vs Bournemouth 경기
('c3333333-3333-3333-3333-333333333333', '0b57b34c-9c62-44b2-8837-c2e215fd6718', DEFAULT);


-- playlists_subscriptions
INSERT INTO playlist_subscriptions (playlist_id, user_id, created_at) VALUES
-- 1. 관리자(u111...)가 '주말 드라마(p111...)' 구독
('c1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', NOW(6)),

-- 2. 관리자(u111...)가 '최신 영화(p222...)' 구독
('c2222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', NOW(6)),

-- 3. 김철수(u222...)가 '스포츠 하이라이트(p333...)' 구독
('c3333333-3333-3333-3333-333333333333', 'a2222222-2222-2222-2222-222222222222', NOW(6)),

-- 4. 이영희(u333...)가 '주말 드라마(p111...)' 구독
('c1111111-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', NOW(6)),

-- 5. 구글러(u555...)가 '최신 영화(p222...)' 구독
('c2222222-2222-2222-2222-222222222222', 'a5555555-5555-5555-5555-555555555555', NOW(6));


-- reviews dummy data
INSERT INTO reviews (id, content_id, author_id, text, rating, created_at, updated_at)
-- 1. 브레이킹 배드에 대한 리뷰
VALUES ('222c1c77-dfa5-4b66-822a-ebc41b8edc82', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a1111111-1111-1111-1111-111111111111', '엄청 몰입감이 좋았습니다.', 5.0, DEFAULT, DEFAULT),
       ('76e8fdc6-4a4c-492e-857b-07a790578e71', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a2222222-2222-2222-2222-222222222222', '인기가 많은 이유가 있네요.', 4.5, DEFAULT, DEFAULT),
       ('8cf87bcc-10a8-4930-867d-35e0c1ee97dc', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a3333333-3333-3333-3333-333333333333', '재미있었습니다.', 4.2, DEFAULT, DEFAULT),
       ('7187ed2d-5e0c-4764-a2a4-57b5fcdb1bdc', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a4444444-4444-4444-4444-444444444444', '왜 명작이라고 꼽히는지 알 거 같아요.', 4.4, DEFAULT, DEFAULT),
       ('58678e29-50b8-4cc5-b4ca-622ef09239e1', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a5555555-5555-5555-5555-555555555555', '적당히 볼만 함.', 3.7, DEFAULT, DEFAULT),
       ('55f92ebd-df63-4190-99a5-0cb43d5b62b7', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a6666666-6666-6666-6666-666666666666', '이거 보는 데 시간 쓸만한 듯', 3.7, DEFAULT, DEFAULT),
       ('8297dfa6-5188-4e15-8650-908686413a0f', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a7777777-7777-7777-7777-777777777777', '그냥저냥', 2.9, DEFAULT, DEFAULT),
       ('d9f96fee-6201-40a9-a80e-7fc67354686c', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a8888888-8888-8888-8888-888888888888', '별로였음', 2.1, DEFAULT, DEFAULT),
       ('0e1cbe05-41a3-4a35-b8a5-3e006addcdc5', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a9999999-9999-9999-9999-999999999999', '나랑 안 맞네.', 1.9, DEFAULT, DEFAULT),
       ('9a303f31-289b-4c96-b619-efa679a86ba8', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a1010101-0101-0101-0101-010101010101', '꿀잼ㅁ', 4.9, DEFAULT, DEFAULT),
       ('aa30e8b0-6bd7-41dc-9435-0b2f1537b32c', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a1212121-2121-2121-2121-212121212121', '시간이 잘 가네요.', 3.7, DEFAULT, DEFAULT),
       ('29e94102-87ff-4e95-b73e-7192b35c39aa', 'a1e21d98-bc59-4682-a78d-cd556457f482', 'a1313131-3131-3131-3131-313131313131', '나중에 또 볼듯?', 3.8, DEFAULT, DEFAULT),

-- 2. Aston Villa vs Bournemouth에 대한 리뷰
       ('1ac28646-59ed-4dce-8f60-1fe33eb5626d', '0b57b34c-9c62-44b2-8837-c2e215fd6718', 'a1111111-1111-1111-1111-111111111111', '지루한 경기였네요.', 2.2, DEFAULT, DEFAULT),
       ('3cb3aa49-87f1-413a-b347-94989cd5e1d4', '0b57b34c-9c62-44b2-8837-c2e215fd6718', 'a2222222-2222-2222-2222-222222222222', '생각보다 치열한 경기였습니다. 재미있었네요.', 4.0, DEFAULT, DEFAULT),

-- 3. 기묘한 이야기에 대한 리뷰
       ('3ba3ac98-532a-4a05-b5a4-4e6a42777815', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'a1111111-1111-1111-1111-111111111111', '재미있었네요.', 4.4, DEFAULT, DEFAULT),
       ('21ccefc5-f34e-4b52-9413-37bcd6075712', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'a2222222-2222-2222-2222-222222222222', '살짝 아쉬웠어요.', 4.3, DEFAULT, DEFAULT),
       ('5a16f118-f8b3-480d-afbe-078d72e158f1', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'a3333333-3333-3333-3333-333333333333', '마지막 시즌만을 기다렸습니다.', 5.0, DEFAULT, DEFAULT),
       ('6882f02e-d4bf-4030-a31e-38fd1018a3fe', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'a4444444-4444-4444-4444-444444444444', '최고입니다.', 5.0, DEFAULT, DEFAULT),
       ('519e3acc-2013-432b-a8af-e509cd111d35', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'a5555555-5555-5555-5555-555555555555', '실망스럽습니다.', 2.1, DEFAULT, DEFAULT),
       ('145e51cb-9cac-451d-9416-b52b7c77089e', 'aa80720d-6fef-48e1-a5f0-14b68c3d106e', 'a6666666-6666-6666-6666-666666666666', '킬링타임용으로 볼만한 거 같아요.', 3.6, DEFAULT, DEFAULT);


-- follows dummy data
INSERT INTO follows(id, follower_id, followee_id, created_at)
VALUES
       -- 1. 관리자가 김철수를 팔로우
       ('bda51673-2e55-42b0-97fc-06ccb0fb5dd9', 'a1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', DEFAULT),
       -- 2. 김철수가 관리자를 팔로우
       ('bda51674-2e55-42b0-97fc-06ccb0fb5dd9', 'a2222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', DEFAULT),
       -- 3. 관리자가 이영희를 팔로우
       ('bda51675-2e55-42b0-97fc-06ccb0fb5dd9', 'a1111111-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', DEFAULT),
       -- 4. 이영희가 김철수를 팔로우
       ('bda51676-2e55-42b0-97fc-06ccb0fb5dd9', 'a3333333-3333-3333-3333-333333333333', 'a2222222-2222-2222-2222-222222222222', DEFAULT);


-- conversations dummy data
INSERT INTO conversations (id, created_at, updated_at)
VALUES ('f33d41a7-71a0-47ae-99a9-3ce712c69c81', DEFAULT, NOW(6)),
       ('d55512f5-ebb5-48ca-84e9-769ef28b6790', DEFAULT, DEFAULT),
       ('8382e9a9-c195-4b48-954d-5990acd66b18', DEFAULT, DEFAULT),
       ('5ab0c10b-7b30-4ab8-9ec2-df20be5861f4', DEFAULT, DEFAULT),
       ('3289a6d1-c23b-4a23-9fcc-d4e6a9d8ed2d', DEFAULT, DEFAULT),
       ('3134b24b-b7b7-49f4-a3e1-1463a81fe936', DEFAULT, DEFAULT);


-- conversation participants dummy data
INSERT INTO conversation_participants (conversation_id, user_id, joined_at, last_read_at)
        -- 대화방 1 참여자
VALUES ('f33d41a7-71a0-47ae-99a9-3ce712c69c81', 'a1111111-1111-1111-1111-111111111111', DEFAULT, NULL),
       ('f33d41a7-71a0-47ae-99a9-3ce712c69c81', 'a2222222-2222-2222-2222-222222222222', DEFAULT, NULL),
        -- 대화방 2 참여자
       ('d55512f5-ebb5-48ca-84e9-769ef28b6790', 'a3333333-3333-3333-3333-333333333333', DEFAULT, NULL),
       ('d55512f5-ebb5-48ca-84e9-769ef28b6790', 'a1111111-1111-1111-1111-111111111111', DEFAULT, NULL),
        -- 대화방 3 참여자
       ('8382e9a9-c195-4b48-954d-5990acd66b18', 'a1111111-1111-1111-1111-111111111111', DEFAULT, NULL),
       ('8382e9a9-c195-4b48-954d-5990acd66b18', 'a4444444-4444-4444-4444-444444444444', DEFAULT, NULL),
        -- 대화방 4 참여자
       ('5ab0c10b-7b30-4ab8-9ec2-df20be5861f4', 'a2222222-2222-2222-2222-222222222222', DEFAULT, NULL),
       ('5ab0c10b-7b30-4ab8-9ec2-df20be5861f4', 'a3333333-3333-3333-3333-333333333333', DEFAULT, NULL),
        -- 대화방 5 참여자
       ('3289a6d1-c23b-4a23-9fcc-d4e6a9d8ed2d', 'a2222222-2222-2222-2222-222222222222', DEFAULT, NULL),
       ('3289a6d1-c23b-4a23-9fcc-d4e6a9d8ed2d', 'a4444444-4444-4444-4444-444444444444', DEFAULT, NULL),
        -- 대화방 6 참여자
       ('3134b24b-b7b7-49f4-a3e1-1463a81fe936', 'a2222222-2222-2222-2222-222222222222', DEFAULT, NULL),
       ('3134b24b-b7b7-49f4-a3e1-1463a81fe936', 'a5555555-5555-5555-5555-555555555555', DEFAULT, NULL);


INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content, created_at, read_at)
VALUES ('56460e9e-d4f1-4ad6-a6fd-9303c40295f1', 'f33d41a7-71a0-47ae-99a9-3ce712c69c81', 'a1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'ㅎㅇ요', NOW(6), NULL),
       ('56460e9e-d4f1-4ad6-a6fd-9303c40295f2', 'f33d41a7-71a0-47ae-99a9-3ce712c69c81', 'a2222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', '안녕하세요.', NOW(6), NULL),
       ('56460e9e-d4f1-4ad6-a6fd-9303c40295f3', 'f33d41a7-71a0-47ae-99a9-3ce712c69c81', 'a1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '뭐함?', NOW(6), NULL),
       ('56460e9e-d4f1-4ad6-a6fd-9303c40295f4', 'f33d41a7-71a0-47ae-99a9-3ce712c69c81', 'a1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '저녁먹음?', NOW(6), NULL);


-- notifications dummy data
INSERT INTO notifications (id, receiver_id, title, content, level, created_at, read_at)
VALUES ('4bff7808-821c-4c7d-9f5b-b884def5c82d', 'a2222222-2222-2222-2222-222222222222', '관리자님이 나를 팔로우했어요.', '', 'INFO', DEFAULT, NULL),
       ('4bff7808-822c-4c7d-9f5b-b884def5c82d', 'a1111111-1111-1111-1111-111111111111', '김철수님이 나를 팔로우했어요.', '', 'INFO', DEFAULT, NULL),
       ('4bff7809-821c-4c7d-9f5b-b884def5c82d', 'a3333333-3333-3333-3333-333333333333', '관리자님이 나를 팔로우했어요.', '', 'INFO', DEFAULT, NULL),
       ('4bff7810-821c-4c7d-9f5b-b884def5c82d', 'a2222222-2222-2222-2222-222222222222', '이영희님이 나를 팔로우했어요.', '', 'INFO', DEFAULT, NULL),
       ('a03ad3b2-a92c-44e5-b287-b66ed8c1bb30', 'a1111111-1111-1111-1111-111111111111', '김철수님이 플레이리스트를 만들었어요.', '주말에 정주행할 드라마', 'INFO', NOW(6), NULL),
       ('a03ad3b2-a92c-44e5-b287-b66ed8c1bb31', 'a1111111-1111-1111-1111-111111111111', '이영희님이 플레이리스트를 만들었어요.', '최신 개봉 영화 기대작', 'INFO', NOW(6), NULL),
       ('a03ad3b2-a92c-44e5-b287-b66ed8c1bb32', 'a3333333-3333-3333-3333-333333333333', '김철수님이 플레이리스트를 만들었어요.', '주말에 정주행할 드라마', 'INFO', NOW(6), NULL),
       ('8b730369-9b14-412b-8e23-53626ba36226', 'a2222222-2222-2222-2222-222222222222', '[DM] 관리자', 'ㅎㅇ요', 'INFO', NOW(6), NULL),
       ('8e709210-29e2-495f-8b8a-ad7cd7cb8b8c', 'a1111111-1111-1111-1111-111111111111', '[DM] 김철수', '안녕하세요.', 'INFO', NOW(6), NULL),
       ('0738bbc9-43a0-4737-916f-835569d11655', 'a2222222-2222-2222-2222-222222222222', '[DM] 관리자', '뭐함?', 'INFO', NOW(6), NULL),
       ('36460e9e-d4f1-4ad6-a6fd-9303c40295f7', 'a2222222-2222-2222-2222-222222222222', '[DM] 관리자', '저녁먹음?', 'INFO', NOW(6), NULL);