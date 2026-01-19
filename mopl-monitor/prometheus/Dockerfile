FROM prom/prometheus:v2.55.1

USER root
RUN apk add --no-cache gettext

COPY mopl-monitor/prometheus/prometheus-staging.yml /etc/prometheus/prometheus.yml.template
COPY mopl-monitor/prometheus/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER nobody
ENTRYPOINT ["/entrypoint.sh"]
