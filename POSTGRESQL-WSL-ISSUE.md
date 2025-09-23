# Problema de Conectividade PostgreSQL WSL-Windows

## Situação Identificada

Durante a configuração do PostgreSQL para o projeto TechNews, foi identificado um problema de conectividade entre o WSL (Windows Subsystem for Linux) e o PostgreSQL instalado no Windows.

## Detalhes do Problema

### PostgreSQL Status
- ✅ PostgreSQL está rodando no Windows (serviço `postgresql-x64-17`)
- ✅ PostgreSQL está escutando na porta 5432 (confirmado via `netstat`)
- ✅ Versão: PostgreSQL 17.6 on x86_64-windows
- ✅ Banco de dados `technews` existe

### Erro Encontrado
```
java.net.ConnectException: Connection refused
```

### Configuração Testada
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/technews
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Causa Provável

O problema ocorre porque o WSL não consegue acessar diretamente o PostgreSQL instalado no Windows host através de `localhost`. Isso é uma limitação conhecida da arquitetura WSL.

## Soluções Possíveis

### 1. Usar IP do Windows Host
```properties
spring.datasource.url=jdbc:postgresql://$(hostname -I | awk '{print $1}'):5432/technews
```

### 2. Configurar PostgreSQL para aceitar conexões WSL
Editar `postgresql.conf` e `pg_hba.conf` para permitir conexões da rede WSL.

### 3. Instalar PostgreSQL no WSL
Instalar PostgreSQL diretamente no ambiente WSL.

### 4. Usar Docker (Recomendado)
Usar PostgreSQL em container Docker que funciona tanto no Windows quanto no WSL.

## Decisão Atual

Por enquanto, o projeto continuará usando H2 em memória para desenvolvimento, que funciona perfeitamente. A configuração PostgreSQL foi preparada e testada, ficando disponível para uso futuro quando a conectividade for resolvida.

## Arquivos Configurados

- ✅ `application-postgres.properties` - Configuração PostgreSQL pronta
- ✅ `application-dev.properties` - Configuração H2 funcionando
- ✅ `application.properties` - Profile ativo: `dev`

## Data da Análise
22/09/2025 - 21:01