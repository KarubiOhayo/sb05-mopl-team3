#!/usr/bin/env bash
set -u
set -o pipefail

INTERVAL_MINUTES="${INTERVAL_MINUTES:-10}"
SLEEP_SECONDS=$((INTERVAL_MINUTES * 60))
LOG_DIR="${LOG_DIR:-mopl-monitor/k6/logs}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

API_SCRIPT="${API_SCRIPT:-mopl-monitor/k6/mopl-api-staging.js}"
PLAYLIST_SCRIPT="${PLAYLIST_SCRIPT:-mopl-monitor/k6/mopl-api-staging-playlists.js}"
REVIEW_SCRIPT="${REVIEW_SCRIPT:-mopl-monitor/k6/mopl-api-staging-reviews.js}"

API_RPS_START_RATE="${API_RPS_START_RATE:-40}"
API_RPS_STAGES="${API_RPS_STAGES:-40:5m,60:5m,80:5m,100:5m}"
API_RPS_PREALLOCATED_VUS="${API_RPS_PREALLOCATED_VUS:-400}"
API_RPS_MAX_VUS="${API_RPS_MAX_VUS:-800}"

PLAYLIST_RPS_START_RATE="${PLAYLIST_RPS_START_RATE:-60}"
PLAYLIST_RPS_STAGES="${PLAYLIST_RPS_STAGES:-60:5m,80:5m,100:5m,120:5m}"
PLAYLIST_RPS_PREALLOCATED_VUS="${PLAYLIST_RPS_PREALLOCATED_VUS:-300}"
PLAYLIST_RPS_MAX_VUS="${PLAYLIST_RPS_MAX_VUS:-600}"

REVIEW_RPS_START_RATE="${REVIEW_RPS_START_RATE:-40}"
REVIEW_RPS_STAGES="${REVIEW_RPS_STAGES:-40:5m,60:5m,80:5m,100:5m}"
REVIEW_RPS_PREALLOCATED_VUS="${REVIEW_RPS_PREALLOCATED_VUS:-300}"
REVIEW_RPS_MAX_VUS="${REVIEW_RPS_MAX_VUS:-600}"

export K6_TEST_USER_COUNT="${K6_TEST_USER_COUNT:-50}"
export K6_TEST_USER_PREFIX="${K6_TEST_USER_PREFIX:-loadtest-}"
export K6_TEST_USER_PASSWORD="${K6_TEST_USER_PASSWORD:-1234}"
export K6_TREND_STATS="${K6_TREND_STATS:-p(90),p(95),p(99),avg,min,med,max}"
export K6_PROMETHEUS_RW_TREND_STATS="${K6_PROMETHEUS_RW_TREND_STATS:-p(90),p(95),p(99),avg,min,med,max}"

OUT_ARGS_RAW="${K6_OUT_ARGS:---out experimental-prometheus-rw}"
OUT_ARGS=()
if [[ -n "${OUT_ARGS_RAW}" ]]; then
  # shellcheck disable=SC2206
  OUT_ARGS=(${OUT_ARGS_RAW})
fi

if [[ "${OUT_ARGS_RAW}" == *experimental-prometheus-rw* ]]; then
  : "${K6_PROMETHEUS_RW_SERVER_URL:?K6_PROMETHEUS_RW_SERVER_URL is required}"
  : "${K6_PROMETHEUS_RW_USERNAME:?K6_PROMETHEUS_RW_USERNAME is required}"
  : "${K6_PROMETHEUS_RW_PASSWORD:?K6_PROMETHEUS_RW_PASSWORD is required}"
  export K6_PROMETHEUS_RW_SERVER_URL
  export K6_PROMETHEUS_RW_USERNAME
  export K6_PROMETHEUS_RW_PASSWORD
fi

mkdir -p "${LOG_DIR}"
FAILURES=()

run_k6() {
  local name="$1"
  local script="$2"
  local start_rate="$3"
  local stages="$4"
  local pre_vus="$5"
  local max_vus="$6"
  local log_file="${LOG_DIR}/${TIMESTAMP}-${name}.log"

  echo "=== $(date '+%F %T') | ${name} | ${script} ===" | tee -a "${log_file}"
  K6_MODE=rps \
  K6_RPS_START_RATE="${start_rate}" \
  K6_RPS_STAGES="${stages}" \
  K6_RPS_PREALLOCATED_VUS="${pre_vus}" \
  K6_RPS_MAX_VUS="${max_vus}" \
  k6 run "${OUT_ARGS[@]}" "${script}" 2>&1 | tee -a "${log_file}"
  local rc=${PIPESTATUS[0]}
  if [[ ${rc} -ne 0 ]]; then
    echo "!!! ${name} failed with exit code ${rc}" | tee -a "${log_file}"
    FAILURES+=("${name}:${rc}")
  fi
}

run_k6 "api" "${API_SCRIPT}" "${API_RPS_START_RATE}" "${API_RPS_STAGES}" "${API_RPS_PREALLOCATED_VUS}" "${API_RPS_MAX_VUS}"
sleep "${SLEEP_SECONDS}"
run_k6 "playlists" "${PLAYLIST_SCRIPT}" "${PLAYLIST_RPS_START_RATE}" "${PLAYLIST_RPS_STAGES}" "${PLAYLIST_RPS_PREALLOCATED_VUS}" "${PLAYLIST_RPS_MAX_VUS}"
sleep "${SLEEP_SECONDS}"
run_k6 "reviews" "${REVIEW_SCRIPT}" "${REVIEW_RPS_START_RATE}" "${REVIEW_RPS_STAGES}" "${REVIEW_RPS_PREALLOCATED_VUS}" "${REVIEW_RPS_MAX_VUS}"

if [[ ${#FAILURES[@]} -gt 0 ]]; then
  echo "Finished with failures: ${FAILURES[*]}"
  exit 1
fi

echo "All tests completed successfully."
