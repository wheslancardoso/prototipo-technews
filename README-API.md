# TechNews Newsletter API

API REST para integração com o sistema de newsletter do TechNews.

## Base URL
```
http://localhost:8080/api/newsletter
```

## Endpoints

### 1. Criar Inscrição
**POST** `/subscribe`

Cria uma nova inscrição na newsletter.

**Request Body:**
```json
{
  "email": "usuario@exemplo.com",
  "nome": "Nome do Usuário",
  "frequencia": "WEEKLY",
  "categorias": "tecnologia,programacao"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Inscrição realizada com sucesso. Verifique seu email para confirmar.",
  "subscriberId": 123,
  "verificationRequired": true
}
```

### 2. Verificar Status de Inscrição
**GET** `/status/{email}`

Verifica o status de uma inscrição.

**Response:**
```json
{
  "subscribed": true,
  "active": true,
  "verified": true,
  "frequency": "WEEKLY",
  "subscriptionDate": "2024-01-15T10:30:00",
  "categories": "tecnologia,programacao"
}
```

### 3. Cancelar Inscrição
**DELETE** `/unsubscribe/{email}`

Cancela uma inscrição existente.

**Query Parameters:**
- `reason` (opcional): Motivo do cancelamento

**Response:**
```json
{
  "success": true,
  "message": "Inscrição cancelada com sucesso"
}
```

### 4. Atualizar Preferências
**PUT** `/preferences/{email}`

Atualiza as preferências de um assinante.

**Request Body:**
```json
{
  "nome": "Novo Nome",
  "frequencia": "DAILY",
  "categorias": "tecnologia,ia,programacao"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Preferências atualizadas com sucesso",
  "subscriber": {
    "id": 123,
    "email": "usuario@exemplo.com",
    "nome": "Novo Nome",
    "ativo": true,
    "emailVerificado": true,
    "frequencia": "DAILY",
    "categorias": "tecnologia,ia,programacao"
  }
}
```

### 5. Listar Assinantes
**GET** `/subscribers`

Lista assinantes com paginação e filtros.

**Query Parameters:**
- `activeOnly` (default: true): Apenas assinantes ativos
- `frequency`: Filtrar por frequência (DAILY, WEEKLY, MONTHLY)
- `search`: Buscar por email ou nome
- `page`: Número da página (default: 0)
- `size`: Tamanho da página (default: 20)

**Response:**
```json
{
  "success": true,
  "subscribers": [...],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "size": 20
}
```

### 6. Obter Estatísticas
**GET** `/stats`

Retorna estatísticas da newsletter.

**Response:**
```json
{
  "success": true,
  "stats": {
    "totalSubscribers": 1500,
    "activeSubscribers": 1200,
    "verifiedSubscribers": 1100,
    "unverifiedSubscribers": 100,
    "frequencyDistribution": {
      "DAILY": 300,
      "WEEKLY": 800,
      "MONTHLY": 100
    },
    "categoryDistribution": {
      "tecnologia": 900,
      "programacao": 700,
      "ia": 400
    },
    "recentSubscriptions": 45,
    "monthlyGrowth": 12.5
  }
}
```

### 7. Enviar Newsletter
**POST** `/send`

Envia newsletter manualmente.

**Request Body:**
```json
{
  "frequency": "WEEKLY",
  "categoryIds": "tecnologia,programacao",
  "testMode": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Newsletter enviada com sucesso",
  "emailsSent": 800,
  "testMode": false
}
```

### 8. Reativar Inscrição
**POST** `/reactivate`

Reativa uma inscrição cancelada.

**Request Body:**
```json
{
  "email": "usuario@exemplo.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Inscrição reativada com sucesso"
}
```

### 9. Verificar Email
**POST** `/verify`

Verifica um email usando token.

**Request Body:**
```json
{
  "token": "abc123def456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Email verificado com sucesso"
}
```

## Frequências Disponíveis
- `DAILY`: Diária
- `WEEKLY`: Semanal  
- `MONTHLY`: Mensal

## Códigos de Erro
- `EMAIL_ALREADY_EXISTS`: Email já está inscrito
- `EMAIL_NOT_FOUND`: Email não encontrado
- `SUBSCRIBER_NOT_FOUND`: Assinante não encontrado
- `INVALID_TOKEN`: Token inválido ou expirado
- `EMAIL_NOT_FOUND_OR_ACTIVE`: Email não encontrado ou já está ativo
- `INTERNAL_ERROR`: Erro interno do servidor

## Rate Limiting
- 60 requisições por minuto
- 1000 requisições por hora

## CORS
A API suporta CORS para todas as origens em desenvolvimento. Em produção, configure as origens permitidas.

## Documentação Swagger
Acesse `/swagger-ui.html` para documentação interativa da API.

## Exemplos de Uso

### JavaScript/Fetch
```javascript
// Criar inscrição
const response = await fetch('/api/newsletter/subscribe', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'usuario@exemplo.com',
    nome: 'João Silva',
    frequencia: 'WEEKLY',
    categorias: 'tecnologia,programacao'
  })
});

const result = await response.json();
console.log(result);
```

### cURL
```bash
# Criar inscrição
curl -X POST http://localhost:8080/api/newsletter/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@exemplo.com",
    "nome": "João Silva",
    "frequencia": "WEEKLY",
    "categorias": "tecnologia,programacao"
  }'

# Verificar status
curl http://localhost:8080/api/newsletter/status/usuario@exemplo.com

# Cancelar inscrição
curl -X DELETE http://localhost:8080/api/newsletter/unsubscribe/usuario@exemplo.com?reason=nao_interessado
```

### Python/Requests
```python
import requests

# Criar inscrição
response = requests.post('http://localhost:8080/api/newsletter/subscribe', 
  json={
    'email': 'usuario@exemplo.com',
    'nome': 'João Silva',
    'frequencia': 'WEEKLY',
    'categorias': 'tecnologia,programacao'
  }
)

print(response.json())
```

## Notas de Implementação
- Todos os endpoints retornam JSON
- Use Content-Type: application/json para requests POST/PUT
- A API é stateless e não requer autenticação por padrão
- Timestamps são retornados no formato ISO 8601
- A paginação segue o padrão Spring Data (page, size, sort)

## Configuração de Email (Mailgun)
- Por padrão, o envio usa SMTP (ex.: Gmail). Opcionalmente, você pode habilitar o envio via API do Mailgun.
- Para evitar expor segredos, todas as credenciais do Mailgun são lidas via variáveis de ambiente.

### Variáveis de ambiente
- `MAILGUN_ENABLED` → `true` para usar a API do Mailgun; `false` usa SMTP.
- `MAILGUN_DOMAIN` → seu domínio no Mailgun (ex.: `sandbox123.mailgun.org`).
- `MAILGUN_API_KEY` → sua chave de API (ex.: `key-xxxxxxxxxxxxxxxxxxxx`).
- `GNEWS_API_KEY` → chave da API GNews (antes era fixa, agora via env).

### Onde colocar as chaves
- Windows PowerShell (persistente):
  - `setx MAILGUN_ENABLED "true"`
  - `setx MAILGUN_DOMAIN "sandbox123.mailgun.org"`
  - `setx MAILGUN_API_KEY "key-xxxxxxxxxxxxxxxxxxxx"`
  - `setx GNEWS_API_KEY "sua_chave_gnews"`
- WSL/Linux (na sessão atual):
  - `export MAILGUN_ENABLED=true`
  - `export MAILGUN_DOMAIN=sandbox123.mailgun.org`
  - `export MAILGUN_API_KEY=key-xxxxxxxxxxxxxxxxxxxx`
  - `export GNEWS_API_KEY=sua_chave_gnews`
- Arquivo `.env` (não versionado):
  - Crie `.env` na raiz com as variáveis acima. O Spring lê propriedades via ambiente; use ferramentas como direnv ou scripts de shell para carregar.

### SMTP com Mailgun (alternativa sem API key exposta)
- Configure:
  - `spring.mail.host=smtp.mailgun.org`
  - `spring.mail.port=587`
  - `spring.mail.username=postmaster@SEU_DOMINIO`
  - `spring.mail.password` via env (ex.: `MAIL_PASSWORD`)
- Mantém chave de API fora do projeto, usando apenas credenciais SMTP.

### Comportamento
- Se `MAILGUN_ENABLED=true` e variáveis válidas, o `EmailService` envia via HTTP API do Mailgun.
- Caso contrário, faz fallback para SMTP (`JavaMailSender`).