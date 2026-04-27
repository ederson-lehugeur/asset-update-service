# InvestAlert API

Backend para monitoramento de oportunidades de investimento em FIIs (Fundos de Investimento Imobiliario). Permite criar regras de monitoramento sobre ativos, receber alertas automaticos por e-mail quando as condicoes sao atendidas e consultar historico de alertas.

## Tecnologias

- Java 25
- Spring Boot 3.5
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA / Hibernate
- MySQL 8.4
- Quartz Scheduler
- SpringDoc OpenAPI (Swagger UI)
- Lombok
- jqwik (property-based testing)
- Docker Compose

## Arquitetura

O projeto segue Clean Architecture com as camadas:

```
domain          - Entidades, value objects, ports (interfaces)
application     - Use cases, commands, responses
adapters        - Controllers (web), persistencia (JPA), e-mail (SMTP), scheduler
infrastructure  - Configuracoes Spring (Security, JWT, Quartz, OpenAPI)
```

## Pre-requisitos

- Java 25+
- Docker e Docker Compose
- Maven 3.9+ (ou use o wrapper `./mvnw`)

## Como rodar

### 1. Subir o banco de dados

```bash
docker compose up -d
```

Isso inicia o MySQL na porta `3307` com o schema e um usuario demo pre-configurados.

### 2. Iniciar a aplicacao

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API estara disponivel em `http://localhost:8080`.

### 3. Acessar o Swagger UI

```
http://localhost:8080/swagger-ui.html
```

A documentacao OpenAPI em JSON esta em:

```
http://localhost:8080/v3/api-docs
```

## Usuario demo

| Campo | Valor                    |
|-------|--------------------------|
| Email | demo@investmonitor.com   |
| Senha | demo123                  |

## Endpoints principais

### Autenticacao (publico)

| Metodo | Endpoint             | Descricao              |
|--------|----------------------|------------------------|
| POST   | `/api/auth/register` | Registrar novo usuario |
| POST   | `/api/auth/login`    | Login (retorna JWT)    |

### Assets (autenticado)

| Metodo | Endpoint              | Descricao                  |
|--------|-----------------------|----------------------------|
| GET    | `/api/assets`         | Listar ativos (paginado)   |
| GET    | `/api/assets/{ticker}`| Buscar ativo por ticker    |

### Rules (autenticado)

| Metodo | Endpoint          | Descricao          |
|--------|-------------------|--------------------|
| POST   | `/api/rules`      | Criar regra        |
| GET    | `/api/rules`      | Listar regras      |
| PUT    | `/api/rules/{id}` | Atualizar regra    |
| DELETE | `/api/rules/{id}` | Remover regra      |

### Rule Groups (autenticado)

| Metodo | Endpoint            | Descricao              |
|--------|---------------------|------------------------|
| POST   | `/api/rule-groups`  | Criar grupo de regras  |
| GET    | `/api/rule-groups`  | Listar grupos          |

### Alerts (autenticado)

| Metodo | Endpoint       | Descricao                              |
|--------|----------------|----------------------------------------|
| GET    | `/api/alerts`  | Historico de alertas (paginado/filtros) |

Todos os endpoints autenticados exigem o header `Authorization: Bearer <token>`.

## Variaveis de ambiente

| Variavel              | Padrao                          | Descricao                    |
|-----------------------|---------------------------------|------------------------------|
| `MYSQL_HOST`          | `localhost`                     | Host do MySQL                |
| `MYSQL_PORT`          | `3306`                          | Porta do MySQL               |
| `MYSQL_DATABASE`      | `investmonitor`                 | Nome do banco                |
| `MYSQL_USERNAME`      | `root`                          | Usuario do banco             |
| `MYSQL_PASSWORD`      | `changeme`                      | Senha do banco               |
| `JWT_SECRET`          | (dev key)                       | Chave secreta para JWT       |
| `JWT_EXPIRATION_MS`   | `86400000`                      | Expiracao do token (ms)      |
| `SMTP_HOST`           | `smtp.example.com`              | Host SMTP                    |
| `SMTP_PORT`           | `587`                           | Porta SMTP                   |
| `SMTP_USERNAME`       | `user@example.com`              | Usuario SMTP                 |
| `SMTP_PASSWORD`       | `changeme`                      | Senha SMTP                   |
| `SCHEDULER_INTERVAL_MS`| `300000`                       | Intervalo de avaliacao (ms)  |

## Testes

```bash
./mvnw test
```

Inclui testes unitarios e property-based tests com jqwik. O banco H2 e usado nos testes de integracao.
