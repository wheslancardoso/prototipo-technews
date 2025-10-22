#!/usr/bin/env bash
set -eu

ENV_FILE="${1:-.env}"
if [ ! -f "$ENV_FILE" ]; then
  echo "Arquivo '$ENV_FILE' não encontrado" >&2
  exit 1
fi

# Remove CRLF (\r) dos arquivos criados no Windows para evitar erros '^M'
TMP_FILE="$(mktemp)"
tr -d '\r' < "$ENV_FILE" > "$TMP_FILE"

set -a
source "$TMP_FILE"
set +a

rm -f "$TMP_FILE"
echo "Variáveis carregadas na sessão atual do shell (CRLF removido)."