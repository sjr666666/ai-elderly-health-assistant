#!/bin/sh
set -eu

latest_source_timestamp() {
  find /app/src -type f -printf '%T@\n' 2>/dev/null | sort -nr | head -n 1
}

mvn -q -DskipTests compile
mvn -q spring-boot:run -Dspring-boot.run.profiles=local &
app_pid=$!
last_timestamp="$(latest_source_timestamp)"

cleanup() {
  kill "$app_pid" 2>/dev/null || true
}
trap cleanup INT TERM EXIT

while kill -0 "$app_pid" 2>/dev/null; do
  sleep 1
  current_timestamp="$(latest_source_timestamp)"
  if [ "$current_timestamp" != "$last_timestamp" ]; then
    mvn -q -DskipTests compile
    last_timestamp="$current_timestamp"
  fi
done

wait "$app_pid"
