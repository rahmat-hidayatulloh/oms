#!/usr/bin/env bash
# =============================================================================
# scripts/deploy.sh — Deploy image OMS ke server remote (dev | staging | prod)
# =============================================================================
# Cara kerja:
#   1. Menyalin docker-compose.yml + .env (dibuat dari secret) ke server.
#   2. Menjalankan `docker compose up -d --pull always` di server dengan image
#      dari GitHub Container Registry (OMS_IMAGE).
#
# Mode uji aman (dipakai sampai server target siap):
#   SKIP_DEPLOY=true            -> log "skip", exit 0 (image tetap sudah ter-push)
#   REQUIRE_DEPLOY=true         -> jika secret deploy belum lengkap, exit 1 (FAIL).
#                                  Harus 'true' untuk production.
#   (default)                    -> secret belum lengkap? skip dengan pesan (exit 0).
#
# Secret env yang dibutuhkan untuk deploy nyata:
#   DEPLOY_HOST, DEPLOY_USER, DEPLOY_PATH, SSH_PRIVATE_KEY
# Dan nilai aplikasi: DB_* REDIS_* JWT_SECRET HEALTH_URL ZIPKIN_ENDPOINT ADMIN_PASSWORD
#
# Contoh (local):
#   ENV_NAME=staging SKIP_DEPLOY=true bash scripts/deploy.sh
# =============================================================================
set -euo pipefail

ENV_NAME="${ENV_NAME:?ENV_NAME wajib di-set (dev|staging|prod)}"
SKIP_DEPLOY="${SKIP_DEPLOY:-false}"
REQUIRE_DEPLOY="${REQUIRE_DEPLOY:-false}"
OMS_IMAGE="${OMS_IMAGE:-ghcr.io/oms/oms:${ENV_NAME}}"
REDIS_DATABASE="${REDIS_DATABASE:-0}"   # 0=dev, 1=staging, 2=prod

echo "==> [deploy:${ENV_NAME}] image=${OMS_IMAGE}"

# ── Mode skip (belum ada server target) ─────────────────────────────
if [ "$SKIP_DEPLOY" = "true" ]; then
  echo "==> [deploy:${ENV_NAME}] SKIP_DEPLOY=true — deploy dilewati (image sudah di-push ke registry)."
  exit 0
fi

# ── Cek prasyarat secret deploy ──────────────────────────────────────
MISSING=""
[ -n "${DEPLOY_HOST:-}" ]        || MISSING="${MISSING} DEPLOY_HOST"
[ -n "${DEPLOY_USER:-}" ]        || MISSING="${MISSING} DEPLOY_USER"
[ -n "${DEPLOY_PATH:-}" ]        || MISSING="${MISSING} DEPLOY_PATH"
[ -n "${SSH_PRIVATE_KEY:-}" ]    || MISSING="${MISSING} SSH_PRIVATE_KEY"
[ -n "${DB_PASSWORD:-}" ]        || MISSING="${MISSING} DB_PASSWORD"
[ -n "${JWT_SECRET:-}" ]         || MISSING="${MISSING} JWT_SECRET"
[ -n "${DB_NAME:-}" ]            || MISSING="${MISSING} DB_NAME"

if [ -n "$MISSING" ]; then
  if [ "$REQUIRE_DEPLOY" = "true" ]; then
    echo "!! [deploy:${ENV_NAME}] GAGAL: secret deploy belum lengkap:$MISSING" >&2
    exit 1
  fi
  echo "==> [deploy:${ENV_NAME}] Secret deploy belum di-set — skip (image sudah di-push ke registry). Set env DEPLOY_HOST/DEPLOY_USER/DEPLOY_PATH/SSH_PRIVATE_KEY dan variabel aplikasi untuk deploy nyata."
  exit 0
fi

# ── Persiapan koneksi SSH ────────────────────────────────────────────
SSH_KEY_FILE="$(mktemp)"
printf '%s\n' "${SSH_PRIVATE_KEY}" > "${SSH_KEY_FILE}"
chmod 600 "${SSH_KEY_FILE}"
trap 'rm -f "${SSH_KEY_FILE}"' EXIT

SSH_OPTS=(
  -i "${SSH_KEY_FILE}"
  -o StrictHostKeyChecking=accept-new
  -o UserKnownHostsFile=/dev/null
  -o LogLevel=ERROR
)
SSH_DEST="${DEPLOY_USER}@${DEPLOY_HOST}"

echo "==> [deploy:${ENV_NAME}] Menyiapkan ${DEPLOY_PATH} di ${SSH_DEST}..."
ssh "${SSH_OPTS[@]}" "${SSH_DEST}" "mkdir -p '${DEPLOY_PATH}'"

# ── Kirim docker-compose.yml ─────────────────────────────────────────
scp "${SSH_OPTS[@]}" docker-compose.yml "${SSH_DEST}:${DEPLOY_PATH}/docker-compose.yml"

# ── Bangun .env remote dari secret (dengan pembatasan karakter '$') ──
#    Catatan: karakter `$`, backtick, dan backslash tidak aman — ganti di value.
TMP_ENV="$(mktemp)"
cat > "${TMP_ENV}" <<EOF
SPRING_PROFILES_ACTIVE=${ENV_NAME}
OMS_IMAGE=${OMS_IMAGE}
DB_HOST=${DB_HOST:-postgres}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
REDIS_HOST=${REDIS_HOST:-redis}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}
REDIS_DATABASE=${REDIS_DATABASE}
RATE_LIMIT_DISTRIBUTED=${RATE_LIMIT_DISTRIBUTED:-true}
HEALTH_URL=${HEALTH_URL:-http://mock-payment:8080/health}
ZIPKIN_ENDPOINT=${ZIPKIN_ENDPOINT:-http://zipkin:9411/api/v2/spans}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-}
ERROR_BASE_URL=${ERROR_BASE_URL:-http://localhost/errors}
EOF
scp "${SSH_OPTS[@]}" "${TMP_ENV}" "${SSH_DEST}:${DEPLOY_PATH}/.env"
rm -f "${TMP_ENV}"

# ── Jalankan deploy di server ────────────────────────────────────────
echo "==> [deploy:${ENV_NAME}] Melakukan 'docker compose up -d --pull always' di remote..."
ssh "${SSH_OPTS[@]}" "${SSH_DEST}" "cd '${DEPLOY_PATH}' && docker compose up -d --pull always"

echo "==> [deploy:${ENV_NAME}] ✅ Selesai. App: http://${DEPLOY_HOST}:8088/api/version | Actuator: :8081/actuator/health"