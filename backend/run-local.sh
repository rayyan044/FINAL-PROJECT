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

# Expo Go connects from the phone using this computer's current LAN address.
# Add local development origins for the current address without storing a
# machine-specific address in backend/.env.
lan_ip=""
if command -v ip >/dev/null 2>&1; then
  lan_ip="$(ip route get 1.1.1.1 2>/dev/null | awk '{for (i = 1; i <= NF; i++) if ($i == "src") {print $(i + 1); exit}}')"
fi

local_origins="http://localhost:8080,http://localhost:8082,http://localhost:8085,http://127.0.0.1:8080,http://127.0.0.1:8082,http://127.0.0.1:8085"
if [[ -n "$lan_ip" ]]; then
  local_origins+=",http://${lan_ip}:8080,http://${lan_ip}:8082,http://${lan_ip}:8085"
  echo "Local mobile API CORS enabled for ${lan_ip}."
fi
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS},${local_origins}"
export CORS_ALLOWED_ORIGINS

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
