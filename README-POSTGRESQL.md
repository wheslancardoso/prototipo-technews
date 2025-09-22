# Configuração PostgreSQL - TechNews

Este documento explica como configurar e usar PostgreSQL como banco de dados para o projeto TechNews.

## 🚀 Configuração Rápida com Docker

### 1. Iniciar PostgreSQL com Docker Compose

```bash
# Iniciar todos os serviços (PostgreSQL, pgAdmin, MailHog)
docker-compose up -d

# Ou apenas o PostgreSQL
docker-compose up -d postgres
```

### 2. Verificar se os serviços estão rodando

```bash
docker-compose ps
```

### 3. Executar a aplicação com perfil PostgreSQL

```bash
# Opção 1: Via Maven
mvn spring-boot:run -Dspring-boot.run.profiles=postgresql

# Opção 2: Via JAR
java -jar target/technews-0.0.1-SNAPSHOT.jar --spring.profiles.active=postgresql

# Opção 3: Variável de ambiente
export SPRING_PROFILES_ACTIVE=postgresql
mvn spring-boot:run
```

## 🔧 Configuração Manual do PostgreSQL

### 1. Instalar PostgreSQL

#### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

#### Windows:
- Baixar do site oficial: https://www.postgresql.org/download/windows/

#### macOS:
```bash
brew install postgresql
```

### 2. Criar banco de dados e usuário

```sql
-- Conectar como superusuário
sudo -u postgres psql

-- Criar usuário
CREATE USER technews_user WITH PASSWORD 'technews_password';

-- Criar banco de dados
CREATE DATABASE technews OWNER technews_user;

-- Conceder privilégios
GRANT ALL PRIVILEGES ON DATABASE technews TO technews_user;

-- Sair
\q
```

### 3. Configurar conexão

As configurações estão no arquivo `application-postgresql.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/technews
spring.datasource.username=technews_user
spring.datasource.password=technews_password
```

## 📊 Ferramentas de Administração

### pgAdmin (Interface Web)

Após executar `docker-compose up -d`:

- **URL**: http://localhost:5050
- **Email**: admin@technews.com
- **Senha**: admin123

### Linha de Comando

```bash
# Conectar ao banco
psql -h localhost -U technews_user -d technews

# Ou via Docker
docker exec -it technews-postgres psql -U technews_user -d technews
```

## 🗄️ Migrações com Flyway

O projeto usa Flyway para gerenciar migrações do banco de dados.

### Arquivos de Migração

- `V1__Create_initial_schema.sql` - Schema inicial
- `V2__Insert_initial_data.sql` - Dados iniciais

### Comandos Flyway

```bash
# Verificar status das migrações
mvn flyway:info

# Executar migrações pendentes
mvn flyway:migrate

# Limpar banco (CUIDADO - apenas desenvolvimento)
mvn flyway:clean
```

## 🔍 Verificação da Configuração

### 1. Testar conexão

```bash
# Via aplicação Spring Boot
curl http://localhost:8080/actuator/health

# Via psql
psql -h localhost -U technews_user -d technews -c "SELECT version();"
```

### 2. Verificar tabelas criadas

```sql
-- Listar todas as tabelas
\dt

-- Verificar estrutura de uma tabela
\d subscribers
```

### 3. Verificar dados iniciais

```sql
-- Verificar categorias
SELECT * FROM categories;

-- Verificar fontes confiáveis
SELECT * FROM trusted_sources;
```

## 🌍 Variáveis de Ambiente

Para produção, use variáveis de ambiente:

```bash
export SPRING_PROFILES_ACTIVE=postgresql
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/technews
export SPRING_DATASOURCE_USERNAME=technews_user
export SPRING_DATASOURCE_PASSWORD=technews_password
export MAIL_HOST=smtp.gmail.com
export MAIL_USERNAME=seu-email@gmail.com
export MAIL_PASSWORD=sua-senha-app
```

## 🚨 Troubleshooting

### Erro de conexão

1. Verificar se PostgreSQL está rodando:
   ```bash
   docker-compose ps
   # ou
   sudo systemctl status postgresql
   ```

2. Verificar logs:
   ```bash
   docker-compose logs postgres
   ```

### Erro de migração

1. Verificar status do Flyway:
   ```bash
   mvn flyway:info
   ```

2. Reparar migrações (se necessário):
   ```bash
   mvn flyway:repair
   ```

### Resetar banco (desenvolvimento)

```bash
# Parar aplicação
# Limpar e recriar banco
docker-compose down -v
docker-compose up -d postgres
# Reiniciar aplicação
```

## 📝 Backup e Restore

### Backup

```bash
# Via Docker
docker exec technews-postgres pg_dump -U technews_user technews > backup.sql

# Via comando local
pg_dump -h localhost -U technews_user technews > backup.sql
```

### Restore

```bash
# Via Docker
docker exec -i technews-postgres psql -U technews_user technews < backup.sql

# Via comando local
psql -h localhost -U technews_user technews < backup.sql
```

## 🔗 Links Úteis

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization)