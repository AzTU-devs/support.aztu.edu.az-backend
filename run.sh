#!/usr/bin/env bash
# Loads environment variables from .env (if present) and starts the API.
# Usage: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
  echo "Loaded environment from .env"
else
  echo "No .env file found — using application.yml defaults (localhost)."
fi

exec ./mvnw spring-boot:run
