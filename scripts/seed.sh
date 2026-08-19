#!/usr/bin/env bash
# Seeds a demo login and a demo mission against a running `docker compose up` stack.
# Safe to re-run: an already-registered username is treated as already-seeded.
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
USERNAME="${SEED_USERNAME:-operator1}"
PASSWORD="${SEED_PASSWORD:-drone-pass-123}"
DRONE_ID="${SEED_DRONE_ID:-drone-001}"

echo "==> Registering demo user '${USERNAME}'"
register_status=$(curl -s -o /tmp/seed_register.json -w '%{http_code}' \
  -X POST "${GATEWAY_URL}/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

if [ "${register_status}" = "201" ]; then
  echo "    created"
elif [ "${register_status}" = "409" ]; then
  echo "    already exists, skipping"
else
  echo "    unexpected status ${register_status}: $(cat /tmp/seed_register.json)" >&2
  exit 1
fi

echo "==> Logging in as '${USERNAME}'"
login_status=$(curl -s -o /tmp/seed_login.json -w '%{http_code}' \
  -X POST "${GATEWAY_URL}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

if [ "${login_status}" != "200" ]; then
  echo "    login failed with status ${login_status}: $(cat /tmp/seed_login.json)" >&2
  exit 1
fi

TOKEN=$(python3 -c "import json;print(json.load(open('/tmp/seed_login.json'))['token'])")

echo "==> Assigning a demo mission to '${DRONE_ID}' (patrol around Riyadh)"
mission_status=$(curl -s -o /tmp/seed_mission.json -w '%{http_code}' \
  -X POST "${GATEWAY_URL}/api/missions" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"droneId\":\"${DRONE_ID}\",\"waypoints\":[
    {\"lat\":24.7136,\"lon\":46.6753,\"altitudeM\":80},
    {\"lat\":24.7300,\"lon\":46.6900,\"altitudeM\":80},
    {\"lat\":24.7250,\"lon\":46.7100,\"altitudeM\":80},
    {\"lat\":24.7050,\"lon\":46.6950,\"altitudeM\":80}
  ]}")

if [ "${mission_status}" = "201" ]; then
  echo "    mission created"
else
  echo "    unexpected status ${mission_status}: $(cat /tmp/seed_mission.json)" >&2
  exit 1
fi

echo "==> Done. Log in at http://localhost:5173 with:"
echo "    username: ${USERNAME}"
echo "    password: ${PASSWORD}"
