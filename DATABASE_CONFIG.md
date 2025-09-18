# Configuração de Banco de Dados - TechNews

## Visão Geral

A aplicação TechNews foi configurada para suportar múltiplos ambientes com diferentes bancos de dados:

- **Desenvolvimento**: H2 Database (em memória)
- **Produção**: PostgreSQL

## Perfis de Configuração

### Perfil de Desenvolvimento (`dev`)

**Arquivo**: `application-dev.properties`

- **Banco**: H2 Database em memória
- **URL**: `jdbc:h2:mem:technews_dev`
- **Console H2**: Habilitado em `/h2-console`
- **Usuário**: `sa` (sem senha)
- **DDL**: `create-drop` (recria tabelas a cada inicialização)
- **Email**: Configuração mock para desenvolvimento local

### Perfil de Produção (`prod`)

**Arquivo**: `application-prod.properties`

- **Banco**: PostgreSQL
- **Configuração via variáveis de ambiente**:
  - `DB_HOST`: Host do PostgreSQL
  - `DB_PORT`: Porta (padrão: 5432)
  - `DB_NAME`: Nome do banco
  - `DB_USERNAME`: Usuário do banco
  - `DB_PASSWORD`: Senha do banco
- **DDL**: `validate` (não altera estrutura)
- **Email**: Configuração real via variáveis de ambiente

## Como Usar

### Executar em Desenvolvimento

```bash
# Usa perfil dev por padrão (configurado em application.properties)
mvn spring-boot:run

# Ou explicitamente
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Executar em Produção

```bash
# Definir perfil de produção
export SPRING_PROFILES_ACTIVE=prod

# Configurar variáveis de ambiente do PostgreSQL
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=technews_prod
export DB_USERNAME=technews_user
export DB_PASSWORD=sua_senha_segura

# Configurar variáveis de email
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=seu_email@gmail.com
export MAIL_PASSWORD=sua_senha_app

# Executar aplicação
mvn spring-boot:run
```

## Dados Iniciais

### Desenvolvimento (H2)
- Arquivo: `data.sql`
- Usa função `NOW()` compatível com H2
- Carrega automaticamente a cada inicialização

### Produção (PostgreSQL)
- Arquivo: `data-postgresql.sql`
- Usa `CURRENT_TIMESTAMP` padrão SQL
- Carregamento controlado via `spring.sql.init.mode`

## Estrutura de Dados

### Categorias Padrão
1. Tecnologia
2. Inteligência Artificial
3. Desenvolvimento
4. Segurança
5. Cloud Computing
6. Mobile
7. Web Development
8. DevOps

### Fontes Confiáveis
- TechCrunch, Ars Technica, Wired, The Verge
- Stack Overflow Blog, GitHub Blog
- AWS Blog, Google Cloud Blog
- InfoQ, DZone

## Console H2 (Desenvolvimento)

Quando executando em desenvolvimento, acesse:
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:technews_dev`
- **Usuário**: `sa`
- **Senha**: (deixar em branco)

## Migração para Produção

1. **Preparar PostgreSQL**:
   ```sql
   CREATE DATABASE technews_prod;
   CREATE USER technews_user WITH PASSWORD 'sua_senha_segura';
   GRANT ALL PRIVILEGES ON DATABASE technews_prod TO technews_user;
   ```

2. **Configurar variáveis de ambiente** (ver seção "Executar em Produção")

3. **Executar com perfil de produção**:
   ```bash
   SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
   ```

## Troubleshooting

### Erro de Conexão H2
- Verificar se o perfil `dev` está ativo
- Console H2 disponível apenas em desenvolvimento

### Erro de Conexão PostgreSQL
- Verificar variáveis de ambiente
- Confirmar que PostgreSQL está rodando
- Testar conectividade: `psql -h $DB_HOST -p $DB_PORT -U $DB_USERNAME -d $DB_NAME`

### Problemas com Dados Iniciais
- **H2**: Dados recarregados automaticamente
- **PostgreSQL**: Verificar `spring.sql.init.mode=always` se necessário recarregar