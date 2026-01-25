#!/usr/bin/env bash
set -euo pipefail

# K6 Prometheus remote_write config
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prometheus-prod-49-prod-ap-northeast-0.grafana.net/api/prom/push}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-2921805}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-}"
export K6_PROMETHEUS_REMOTE_WRITE_PUSH_INTERVAL="${K6_PROMETHEUS_REMOTE_WRITE_PUSH_INTERVAL:-10s}"
export K6_TREND_STATS="${K6_TREND_STATS:-p(90),p(95),p(99),avg,min,med,max}"
export K6_PROMETHEUS_RW_TREND_STATS="${K6_PROMETHEUS_RW_TREND_STATS:-${K6_TREND_STATS}}"

if [[ -z "${K6_PROMETHEUS_RW_PASSWORD}" ]]; then
  echo "K6_PROMETHEUS_RW_PASSWORD is required"
  exit 1
fi

SCRIPT_PATH="${1:-mopl-monitor/k6/mopl-api-staging.js}"

echo "Running k6 with remote_write:"
echo "  script: ${SCRIPT_PATH}"
echo "  url: ${K6_PROMETHEUS_RW_SERVER_URL}"
echo "  user: ${K6_PROMETHEUS_RW_USERNAME}"
echo "  push interval: ${K6_PROMETHEUS_REMOTE_WRITE_PUSH_INTERVAL}"
echo "  trend stats: ${K6_PROMETHEUS_RW_TREND_STATS}"

k6 run --out experimental-prometheus-rw "${SCRIPT_PATH}"
