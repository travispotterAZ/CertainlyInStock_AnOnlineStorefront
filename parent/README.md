# Certainly In Stock

## Run with Docker Compose

From this directory, build and start the complete stack:

```sh
docker-compose build
docker-compose up
```

Each Dockerfile compiles its service with Maven and Java 21, then runs the
executable JAR in a Java 21 runtime image. No local JAR build is required.

| Container | Host port |
| --- | --- |
| configserver | 8080 |
| auth-service | 8081 |
| user-service | 8082 |
| product-service | 8083 |
| cart-service | 8084 |
| payment-service | 8085 |
| database (PostgreSQL) | 5432 |

All Java containers listen on port 8080 internally. Config Server uses its bundled
`config-repo` with the `native` profile. It waits for PostgreSQL; each application
service waits for both PostgreSQL and Config Server health checks before starting.
Compose enables the `database` profile and requires Config Server for the five
application services.

`init.sql` creates `auth_db`, `user_db`, `product_db`, `cart_db`, and `payment_db`.
The local database username is `postgres` and password is `password`. There are
no entity tables yet.
PostgreSQL data persists in the `postgres-data` volume. Initialization SQL runs
only when that volume is empty; changing the SQL does not update existing data.

Check Config Server at `http://localhost:8080/auth-service/database` and application
health at `http://localhost:8081/actuator/health` (or ports 8082–8085).

Use `docker compose logs` for logs and `docker compose down` to stop the stack
while retaining database data.

## Run locally without Docker

Build all modules from this directory with:

```sh
mvn install
```

The five service modules currently start and run their context tests without a
database or Config Server:

- Config Server is optional. The services try `http://localhost:8888` and continue
  if it is unavailable. Set `CONFIG_SERVER_URL` to use another address. Connection
  warnings are expected when the server is not running.
- Database auto-configuration is disabled unless the Spring profile `database`
  is active. The JPA and PostgreSQL dependencies remain available.

When a database is ready, set `SPRING_PROFILES_ACTIVE=database` and provide
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` for each service. For example:

```sh
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=database
```

The datasource settings can also come from Config Server.
To require Config Server locally, override `spring.config.import` with
`configserver:http://localhost:8888` (without `optional:`).

These are Spring runtime settings, not Maven profiles. Tests run with the
`database` profile will also require a configured, reachable database.
