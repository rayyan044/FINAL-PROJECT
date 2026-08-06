#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if [[ ! -f .env ]]; then
  echo "Missing backend/.env. Copy .env.example to .env and replace every placeholder." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source ./.env
set +a

required=(DB_USERNAME DB_PASSWORD JWT_SECRET BOOTSTRAP_ADMIN_PASSWORD CORS_ALLOWED_ORIGINS)
missing=()
for variable in "${required[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    missing+=("${variable}")
  fi
done

if (( ${#missing[@]} > 0 )); then
  printf 'Missing required environment variables: %s\n' "${missing[*]}" >&2
  exit 1
fi

exec ./mvnw spring-boot:run
