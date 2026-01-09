import http from "k6/http";             // K6의 HTTP 요청 기능을 가져온다.
import {check, sleep} from "k6";      // check: 응답 검증용(상태 코드 확인 등), sleep: 요청 사이에 쉬는 시간 넣기

export const options = {
  stages: [
    // 계단식 부하 테스트 사용자가 늘어났다가 줄었다
    // duration: "30s" → 30초 동안 테스트
    {duration: "30s", target: 20},
    {duration: "30s", target: 50},
    {duration: "30s", target: 100},
    {duration: "30s", target: 200},
    {duration: "30s", target: 0},
  ],
  thresholds: {
    // thresholds → 성능 기준: 응답시간 95%가 500ms 이하 목표
    http_req_duration: ["p(95)<500"],
  },
};

// 환경변수 BASE_URL이 있으면 그걸 쓰고 없으면 기본으로 http://host.docker.internal:8080 사용 (Docker 컨테이너에서 로컬 서버 접속용)
const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";
// 여기에 Postman 토큰 직접 붙여도 됨
const TOKEN = __ENV.TOKEN || "Postman에서 발급 받은 토큰 넣기";

// K6가 반복 실행하는 “메인 함수” 시작
export default function () {
  const headers = {
    Authorization: `Bearer ${TOKEN}`,
  }
  // 플레이리스트 목록 API로 GET 요청 (테스트할 엔드포인트 값 입력)
  const res = http.get(
      `${BASE_URL}/api/playlists?limit=20&sortBy=updatedAt&sortDirection=DESCENDING`,
      {headers}
  );
  if (__ITER === 0) {
    console.log(`status=${res.status} body=${res.body}`);
  }

  // 응답 상태가 200인지 검사, 실패하면 테스트 결과에 오류로 찍힘
  check(res, {"list 200": (r) => r.status === 200});
  // 1초 쉬고 다음 반복
  sleep(1);
}

// 실행 명령(파일 경로만 맞춰서):
// docker run --rm -i -e BASE_URL="http://host.docker.internal:8080" -v ${PWD}:/scripts grafana/k6 run /scripts/원하는_경로/k6-playlist.js  // 원하는_경로 k6-playlist.js의 위치
// docker run --rm -i -e BASE_URL="http://host.docker.internal:8080" -v "C:\dev_source\codeit_final_project\sb05-mopl-team3:/scripts" grafana/k6 run /scripts/mopl-api/src/main/java/io/mopl/api/playlist/test/k6-playlist.js