#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
TOPICS_FILE="$ROOT_DIR/mopl-core/src/main/java/io/mopl/core/kafka/KafkaTopics.java"

: "${KAFKA_BOOTSTRAP:?KAFKA_BOOTSTRAP is required}"
: "${KAFKA_API_KEY:?KAFKA_API_KEY is required}"
: "${KAFKA_API_SECRET:?KAFKA_API_SECRET is required}"

KAFKA_TOPIC_PARTITIONS=${KAFKA_TOPIC_PARTITIONS:-3}
KAFKA_TOPIC_REPLICATION=${KAFKA_TOPIC_REPLICATION:-3}

if [[ -n "${KAFKA_BIN:-}" ]]; then
  KAFKA_TOPICS="$KAFKA_BIN/kafka-topics.sh"
else
  KAFKA_TOPICS="kafka-topics.sh"
fi

if [[ ! -f "$TOPICS_FILE" ]]; then
  echo "Kafka topics file not found: $TOPICS_FILE" >&2
  exit 1
fi

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

CLIENT_PROPS="$TMP_DIR/client.properties"
cat > "$CLIENT_PROPS" <<EOP
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_API_KEY}" password="${KAFKA_API_SECRET}";
ssl.endpoint.identification.algorithm=https
EOP

mapfile -t TOPICS < <(
  awk '/public static final String/ { if (match($0, /"([^"]+)"/, a)) print a[1] }' "$TOPICS_FILE" | sort -u
)

if [[ ${#TOPICS[@]} -eq 0 ]]; then
  echo "No topics found in $TOPICS_FILE" >&2
  exit 1
fi

for topic in "${TOPICS[@]}"; do
  if "$KAFKA_TOPICS" --bootstrap-server "$KAFKA_BOOTSTRAP" --command-config "$CLIENT_PROPS" --describe --topic "$topic" >/dev/null 2>&1; then
    echo "exists: $topic"
  else
    echo "create: $topic"
    "$KAFKA_TOPICS" \
      --bootstrap-server "$KAFKA_BOOTSTRAP" \
      --command-config "$CLIENT_PROPS" \
      --create \
      --if-not-exists \
      --topic "$topic" \
      --partitions "$KAFKA_TOPIC_PARTITIONS" \
      --replication-factor "$KAFKA_TOPIC_REPLICATION"
  fi
done
