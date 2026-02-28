#!/usr/bin/env bash
# Waits for ScyllaDB to accept CQL connections, then applies schema.cql.
# Designed to run as a one-shot init container alongside the scylla service.

set -euo pipefail

HOST="${SCYLLA_HOST:-scylla}"
PORT="${SCYLLA_PORT:-9042}"
SCHEMA_FILE="${SCHEMA_FILE:-/scylla/schema.cql}"
MAX_ATTEMPTS=30
SLEEP_SECONDS=5

echo "Waiting for ScyllaDB at ${HOST}:${PORT} ..."

for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
    if cqlsh "${HOST}" "${PORT}" -e "DESCRIBE CLUSTER" > /dev/null 2>&1; then
        echo "ScyllaDB is ready (attempt ${attempt})."
        break
    fi
    if [ "${attempt}" -eq "${MAX_ATTEMPTS}" ]; then
        echo "ERROR: ScyllaDB did not become ready after $((MAX_ATTEMPTS * SLEEP_SECONDS))s. Aborting."
        exit 1
    fi
    echo "  Not ready yet, retrying in ${SLEEP_SECONDS}s ... (${attempt}/${MAX_ATTEMPTS})"
    sleep "${SLEEP_SECONDS}"
done

echo "Applying schema from ${SCHEMA_FILE} ..."
cqlsh "${HOST}" "${PORT}" -f "${SCHEMA_FILE}"
echo "Schema applied successfully."
