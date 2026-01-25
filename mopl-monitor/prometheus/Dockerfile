FROM prom/prometheus:v2.55.1

COPY mopl-monitor/prometheus/prometheus-staging.yml /etc/prometheus/prometheus.yml.template
COPY mopl-monitor/prometheus/entrypoint.sh /entrypoint.sh

USER nobody
ENTRYPOINT ["/bin/sh", "/entrypoint.sh"]
