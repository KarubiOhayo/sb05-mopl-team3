#!/bin/sh
set -eu

escape_sed() {
  printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'
}

REMOTE_WRITE_URL_ESCAPED="$(escape_sed "${GRAFANA_CLOUD_PROM_REMOTE_WRITE_URL:-}")"
PROM_USERNAME_ESCAPED="$(escape_sed "${GRAFANA_CLOUD_PROM_USERNAME:-}")"
PROM_PASSWORD_ESCAPED="$(escape_sed "${GRAFANA_CLOUD_PROM_PASSWORD:-}")"

sed \
  -e "s|\${GRAFANA_CLOUD_PROM_REMOTE_WRITE_URL}|${REMOTE_WRITE_URL_ESCAPED}|g" \
  -e "s|\${GRAFANA_CLOUD_PROM_USERNAME}|${PROM_USERNAME_ESCAPED}|g" \
  -e "s|\${GRAFANA_CLOUD_PROM_PASSWORD}|${PROM_PASSWORD_ESCAPED}|g" \
  /etc/prometheus/prometheus.yml.template > /etc/prometheus/prometheus.yml

exec /bin/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  "$@"
