# TaskFlow

![CI](https://github.com/Krutika9021/taskflow-microservices/actions/workflows/ci.yml/badge.svg)

A multi-tenant task manager built as three Spring Boot microservices behind an API gateway. Each company (tenant) can only see its own tasks. Built to practise real microservice concerns: JWT security at the edge, tenant isolation, a database per service, containers and CI.

## Architecture

```
Client (curl / Postman)
        |
        v
  API Gateway :8080  -- validates JWT, strips client X-* headers,
        |               adds X-User-Id / X-Tenant-Id / X-Role
        |
   +----+-----------------+
   |                      |
   v                      v
auth-service :8081     task-service :8082
(register, login,      (tasks CRUD, always
 issues JWT)            scoped by tenant)
   |                      |
   v                      v
 auth DB (PostgreSQL)   task DB (PostgreSQL)
```

Only the gateway publishes a port. The services and databases are reachable only inside the Docker network.

## Tech stack

Java 21, Spring Boot 3.5, Spring Cloud Gateway, Spring Security, Spring Data JPA, PostgreSQL 16, jjwt 0.12 (HS256), Maven (wrapper included), Docker and Docker Compose, GitHub Actions, JUnit 5, Mockito, MockMvc, H2 (tests only).

## Run it

Requires Docker.

```bash
cp .env.example .env      # on Windows PowerShell: copy .env.example .env
docker compose up --build
```

Wait until the gateway is up, then call it on `http://localhost:8080`.

## Demo

```bash
# 1. Register a tenant and its admin user
curl -X POST localhost:8080/auth/register -H "Content-Type: application/json" \
  -d '{"email":"admin@acme.test","password":"password1","tenantName":"Acme"}'

# 2. Log in and copy the token
curl -X POST localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@acme.test","password":"password1"}'

# 3. Create and list tasks
curl -X POST localhost:8080/tasks -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"title":"First task"}'
curl localhost:8080/tasks -H "Authorization: Bearer $TOKEN"

# 4. Without a token the gateway answers 401
curl -i localhost:8080/tasks
```

## How tenant isolation works

- The tenant id comes only from the signed JWT, never from the request body.
- `TaskRepository` extends `Repository`, not `JpaRepository`, so there is no unscoped `findById` or `findAll`. Every read and delete method takes a `tenantId`.
- A task that belongs to another tenant returns **404**, not 403, so its existence is not revealed.
- Only an ADMIN can delete a task. The tenant is checked first, so a user from another tenant gets 404, not 403.

## How the gateway prevents header spoofing

task-service trusts the `X-Tenant-Id`, `X-User-Id` and `X-Role` headers, so the gateway removes any such headers sent by the client and sets them from the validated token. task-service is not published outside the Docker network, so requests cannot bypass the gateway.

## Security decisions

- Passwords are hashed with BCrypt.
- Login returns the same generic message for an unknown email and a wrong password, to avoid revealing which emails exist.
- Email is unique across all tenants, because login identifies a user by email only.
- The JWT secret comes from an environment variable and the service refuses to start if it is shorter than 32 bytes.
- Roles are an enum; a token with an unknown role is rejected.

## Tests

- JwtService: claims round trip, expired, tampered, wrong secret and unknown role are all rejected.
- Auth: duplicate email and wrong password (Mockito), register then login (MockMvc on H2).
- Task service: cross-tenant read returns 404, cross-tenant list is empty, non-admin delete returns 403.
- Gateway filter: valid, expired, missing token and spoofed headers.
- End to end: GitHub Actions starts the whole system with Docker Compose and runs `scripts/smoke-test.sh` (register, login, create and list tasks, 401 without a token, tenant isolation, spoofed header ignored).

Run the unit tests locally with `./mvnw verify` (`.\mvnw.cmd verify` on Windows).

## Future work (not implemented)

- Flyway database migrations (schema is currently created by Hibernate `ddl-auto`)
- Kafka events and a notification service
- Redis caching and gateway rate limiting
- Prometheus and Grafana monitoring
- Testcontainers integration tests against real PostgreSQL