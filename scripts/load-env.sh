#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-.env}"
if [ ! -f "$ENV_FILE" ]; then
  echo "Arquivo '$ENV_FILE' não encontrado" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

echo "Variáveis carregadas na sessão atual do shell."