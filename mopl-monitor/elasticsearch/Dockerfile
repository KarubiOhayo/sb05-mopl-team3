ARG ES_VERSION=9.2.2
FROM docker.elastic.co/elasticsearch/elasticsearch:${ES_VERSION}

COPY --chown=elasticsearch:elasticsearch mopl-monitor/elasticsearch/elasticsearch-staging.yml /usr/share/elasticsearch/config/elasticsearch.yml
