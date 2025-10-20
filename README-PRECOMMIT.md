# Pre-commit: proteção de segredos

Este repositório usa **pre-commit** para evitar que segredos sejam commitados.

## Instalação

1. Instale o pre-commit:
   - Windows: `python -m pip install pre-commit`
   - Linux/macOS: `pip install pre-commit`

2. Instale os hooks no repositório:
   - `pre-commit install`  
   Isso cria/atualiza `.git/hooks/pre-commit` para usar a configuração do arquivo `.pre-commit-config.yaml`.

3. (Opcional) Rodar em todos os arquivos:
   - `pre-commit run --all-files`

## O que é verificado

- Hook local `forbid-secrets`:
  - Bloqueia commits com padrões como `MAILGUN_API_KEY=...`, `GNEWS_API_KEY=...`, `GMAIL_APP_PASSWORD=...`, `SECRET_KEY=...` e valores tipo `key-XXXXXXXX`.
  - Bloqueia arquivos `.env`, `*.env` e `*.local.properties`.
  - Ignora documentação e exemplos: `.md`, `.txt`, `.rst`, `README*`, `.env.example`, `instrucoes.md`.

- Hooks adicionais (`pre-commit-hooks`):
  - `detect-private-key`, `check-merge-conflict`, `end-of-file-fixer`, `trailing-whitespace`.

## Observações

- Se você já possui um hook custom em `.git/hooks/pre-commit`, execute `pre-commit install -f` para sobrescrever.
- Em CI/CD, adicione um passo `pre-commit run --all-files` (ou use https://pre-commit.ci/) para garantir a mesma proteção no pipeline.
- Nunca commit secrets; use variáveis de ambiente (`.env` local) e ensure `.gitignore` mantém arquivos sensíveis fora do VCS.