# TechNews – MVP de Apresentação

Este repositório contém um protótipo funcional (MVP) do TechNews, com backend em Spring Boot, banco de dados H2 (dev) e páginas estáticas de apresentação para navegação.

## Arquitetura
- Backend: Spring Boot (Java), APIs REST para newsletter.
- Banco de dados: H2 em memória no perfil `dev` (automático); PostgreSQL em perfis `postgres`/`prod`.
- Frontend: páginas estáticas servidas pelo Spring (`src/main/resources/static`).

## Rotas Principais
- GET `/api/newsletter/email/preview`: retorna HTML da newsletter.
- GET `/api/newsletter/email/smoke`: executa teste de envio (Mailgun/SMTP). Requer variáveis no `.env`.
- GET `/h2-console`: console do H2 (apenas no dev).
- Páginas estáticas:
  - `/static/index.html`: home da demo.
  - `/static/newsletter.html`: preview da newsletter + botão de smoke.

## Executando para Apresentação
1. Build: `mvn -q -DskipTests package`
2. Executar (porta 8083):
   - `java -jar .\target\technews-0.0.1-SNAPSHOT.jar --server.port=8083 --spring.profiles.active=dev`
3. Abrir no navegador:
   - `http://localhost:8083/static/index.html`
   - `http://localhost:8083/static/newsletter.html`

## Mailgun (Opcional para envio real)
Defina no `.env`:
```
MAILGUN_ENABLED=true
MAILGUN_DOMAIN=mg.seu-dominio.com
MAILGUN_API_KEY=key-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
MAIL_FROM=remetente@seu-dominio.com
MAILGUN_TEST_TO=destinatario@seu-dominio.com
```
Carregue e reinicie:
```
./scripts/load-env.ps1
java -jar .\target\technews-0.0.1-SNAPSHOT.jar --server.port=8083 --spring.profiles.active=dev
```

## Observações
- Perfil `dev` usa H2 e `ddl-auto=create-drop`, ideal para demonstração.
- Para produção, usar PostgreSQL e ajustar DNS (SPF, DKIM, DMARC) conforme `EMAIL-DELIVERABILITY.md`.
- Este MVP prioriza usabilidade e validação de caminho; nem todas as funcionalidades estão completas.