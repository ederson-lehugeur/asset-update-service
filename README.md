# AssetUpdateService

Microsservico responsavel por consumir eventos de atualizacao de ativos via RabbitMQ e persistir as variacoes de preco, dividend yield e P/VP no banco de dados MySQL.

## Tecnologias

- Java 25
- Spring Boot 3.5
- Spring AMQP (RabbitMQ)
- Spring Data JPA / Hibernate
- MySQL 8.4
- Lombok
- jqwik (property-based testing)
- Testcontainers
- Docker Compose

## Arquitetura

O projeto segue Clean Architecture com as seguintes camadas:

```
domain          - Entidades (Asset), eventos (UpdateAssetsEvent), ports (interfaces)
application     - Use case (UpdateAssetsUseCase), resultado de processamento (ProcessingResult)
adapters        - Listener RabbitMQ (AssetUpdateListener)
infrastructure  - Configuracoes Spring (RabbitMQ, Retry), adaptadores JPA
```

### Fluxo principal

```
RabbitMQ (invest.assets.update.queue)
    -> AssetUpdateListener
        -> UpdateAssetsUseCaseImpl
            -> PriceVariationEngine (aplica variacao aleatoria)
            -> AssetRepository (persiste no MySQL)
```

### Dead Letter Queue

Mensagens que falham apos as tentativas de retry sao encaminhadas para a DLQ:

- Exchange principal: `invest.assets.exchange` (Direct)
- Fila principal: `invest.assets.update.queue`
- DLX: `invest.assets.dlx.exchange` (Fanout)
- DLQ: `invest.assets.update.dlq`

## Pre-requisitos

- Java 25+
- Docker e Docker Compose
- Maven 3.9+ (ou use o wrapper `./mvnw`)

## Como rodar (standalone)

Este servico pode ser executado de forma independente, sem depender dos demais servicos do projeto.

### 1. Subir as dependencias

```bash
docker compose up -d
```

Isso inicia:
- **MySQL 8.4** na porta `3307`, com schema e dados de seed carregados automaticamente via `docker/mysql/init/`
- **RabbitMQ** nas portas `5672` (AMQP) e `15672` (Management UI: http://localhost:15672)

### 2. Iniciar a aplicacao

```bash
./mvnw spring-boot:run
```

A aplicacao conecta ao MySQL em `localhost:3307` e ao RabbitMQ em `localhost:5672` por padrao.

## Banco de dados

Os scripts de inicializacao em `docker/mysql/init/` sao executados automaticamente na primeira vez que o container MySQL e criado:

| Arquivo                    | Descricao                              |
|----------------------------|----------------------------------------|
| `00-schema.sql`            | DDL completo (tabelas, indices, FKs)   |
| `01-seed-demo-user.sql`    | Usuario demo para desenvolvimento local |
| `02-seed-assets.sql`       | Ativos FII para testes                 |

> Para recriar o banco do zero, remova o volume: `docker compose down -v && docker compose up -d`

## Variaveis de ambiente

| Variavel                              | Padrao              | Descricao                              |
|---------------------------------------|---------------------|----------------------------------------|
| `SPRING_RABBITMQ_HOST`                | `localhost`         | Host do RabbitMQ                       |
| `SPRING_RABBITMQ_PORT`                | `5672`              | Porta AMQP do RabbitMQ                 |
| `SPRING_RABBITMQ_USERNAME`            | `guest`             | Usuario do RabbitMQ                    |
| `SPRING_RABBITMQ_PASSWORD`            | `guest`             | Senha do RabbitMQ                      |
| `RABBITMQ_LISTENER_PREFETCH`          | `10`                | Prefetch count do listener             |
| `RABBITMQ_LISTENER_CONCURRENCY`       | `1`                 | Concorrencia minima do listener        |
| `RABBITMQ_LISTENER_MAX_CONCURRENCY`   | `3`                 | Concorrencia maxima do listener        |
| `MYSQL_HOST`                          | `localhost`         | Host do MySQL                          |
| `MYSQL_PORT`                          | `3307`              | Porta do MySQL                         |
| `MYSQL_DATABASE`                      | `investalert`       | Nome do banco de dados                 |
| `MYSQL_USERNAME`                      | `root`              | Usuario do banco                       |
| `MYSQL_PASSWORD`                      | `root`              | Senha do banco                         |
| `TZ`                                  | `America/Sao_Paulo` | Timezone da aplicacao                  |

## Formato do evento

O servico espera mensagens no seguinte formato JSON na fila `invest.assets.update.queue`:

```json
{
  "eventType": "UPDATE_ASSETS",
  "correlationId": "uuid-v4",
  "data": {
    "assets": ["MXRF11", "HGLG11", "XPML11"]
  }
}
```

Mensagens com `eventType` diferente de `UPDATE_ASSETS` ou com lista de ativos vazia sao descartadas (nack sem requeue).

## Testes

```bash
./mvnw test
```

A suite inclui:

- Testes unitarios para logica de dominio e use cases
- Property-based tests com jqwik (`PriceVariationEngineProperties`, `UpdateAssetsUseCaseProperties`)
- Testes de integracao com Testcontainers e MySQL real (`MysqlEncoding*`)
- H2 em memoria para testes de unidade que envolvem persistencia

## Build da imagem Docker

```bash
docker build -t asset-update-service .
```

O `Dockerfile` usa multi-stage build: compila com `eclipse-temurin:25-jdk` e gera a imagem final com `eclipse-temurin:25-jre`.
