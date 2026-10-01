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
The local database username is `postgres` and password is `password`.
`init.sql` also applies `database/product.sql` to `product_db`, creating the
`product` and `inventory` tables. Table definitions live outside the services;
Product JPA mappings validate the schema instead of creating or altering it.
PostgreSQL data persists in the `postgres-data` volume. Initialization SQL runs
only when that volume is empty; changing the SQL does not update existing data.

For an existing volume whose `product_db` does not yet contain these tables,
apply the schema once from this directory, before starting the updated product service:

```sh
docker compose up -d database
docker compose exec -T database psql -U postgres -d product_db -v ON_ERROR_STOP=1 < database/product.sql
```

This script creates new tables; it does not migrate existing tables. Keep future
schema changes in separate SQL migrations instead of resetting the data volume.

Check Config Server at `http://localhost:8080/auth-service/database` and application
health at `http://localhost:8081/actuator/health` (or ports 8082–8085).

Use `docker compose logs` for logs and `docker compose down` to stop the stack
while retaining database data.

## Run locally without Docker

Build all modules from this directory with:

```sh
mvn install
```

Auth, user, cart, and payment services can start without a database or Config
Server. Product service always requires a database:

- Config Server is optional. The services try `http://localhost:8888` and continue
  if it is unavailable. Set `CONFIG_SERVER_URL` to use another address. Connection
  warnings are expected when the server is not running.
- For auth, user, cart, and payment, database auto-configuration is disabled unless
  the Spring profile `database` is active.
- Product service connects to `jdbc:postgresql://localhost:5432/product_db` by default,
  using the Compose development credentials (`postgres` / `password`). Override
  `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
  `SPRING_DATASOURCE_PASSWORD` as needed. Compose supplies the internal hostname
  `database` instead of `localhost`. Product needs no active profile to use JPA.

For the other application services, set `SPRING_PROFILES_ACTIVE=database` and provide
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

## Product CRUD

Product endpoints are always enabled. The base URL in Compose is
`http://localhost:8083/certainlyinstock/product-service`. Business endpoints use
`/certainlyinstock/<service-name>/...`; health checks remain at `/actuator/health`.

| Method | Path | Result |
| --- | --- | --- |
| POST | `/products` | Create; returns 201 with the product and a Location header |
| GET | `/products` | List products |
| GET | `/products/{pid}` | Read one product |
| PUT | `/products/{pid}` | Replace its writable fields |
| DELETE | `/products/{pid}` | Delete; returns 204 |

The controller accepts the Product entity directly; no request DTO is used.
Example POST or PUT body:

```json
{
  "category": "Books",
  "image": "https://example.com/book.png",
  "price": 19.99,
  "status": true
}
```

Responses also contain the generated integer `pid`, `dateAdded`, and `lastUpdated`
as ISO dates (`YYYY-MM-DD`). Creation sets both dates; updates preserve `dateAdded`
and set `lastUpdated` to the current date. PUT replaces all four writable fields;
omitted fields become null. Any IDs or audit dates in the body are ignored; PUT
uses the ID in the URL. Missing product IDs return 404.

SQL `price` is unconstrained `NUMERIC`, mapped to Java `BigDecimal` without imposing
a scale. `status` is `BOOLEAN` / `Boolean`; the dates are `DATE` / `LocalDate`.
Category and image use unrestricted `VARCHAR` / `String`. PostgreSQL folds the
unquoted table and column names to lowercase (for example, `product.date_added`).

Inventory has one row per product: `pid` is both its primary key and a foreign key
to `product.pid`, `qty` is `INTEGER`, and `backorder` is `BOOLEAN`. A product referenced
by inventory cannot be deleted; the API returns 409. Inventory has no CRUD endpoints yet.

Run product tests with `./mvnw -f product-service/pom.xml test` from this directory.
The CRUD integration tests use H2 in PostgreSQL mode and the same external SQL file;
they do not require Docker or Config Server. Both product test classes activate
the `test` profile to use an isolated H2 database rather than the running PostgreSQL
database.
