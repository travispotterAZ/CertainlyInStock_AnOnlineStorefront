# Certainly In Stock

## Run with Docker Compose

From this directory, build and start the complete stack:

```sh
docker compose up --build -d
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
| order-service (payments API) | 8085 |
| api-gateway (single entry point) | 8086 |
| eureka1 | 8761 |
| eureka2 | 8762 |
| eureka3 | 8763 |
| database (PostgreSQL) | 5432 |
| keycloak (login, tokens, admin console) | 8180 |
| mailpit (dev email inbox: web UI / SMTP) | 8025 / 1025 |

All Java containers listen on port 8080 internally. Config Server uses its bundled
`config-repo` with the `native` profile. It waits for PostgreSQL. The three Eureka
peers then load `eureka-server.properties` and their respective `peer1`, `peer2`, or
`peer3` configuration from Config Server. Each peer registers with and replicates
to the other two; they start concurrently without depending on each other's health.
The application services wait for PostgreSQL, Config Server, and all three peers.
The gateway waits for Config Server and the peers. Compose enables the `database`
profile and requires Config Server for the five application services.

All services, including Config Server and the gateway, register with Eureka and
fetch the registry. Their `eureka.client.serviceUrl.defaultZone` includes
`http://eureka1:8080/eureka/`, `http://eureka2:8080/eureka/`, and
`http://eureka3:8080/eureka/`, allowing clients to fail over between peers.
Config Server starts independently of discovery so the peers can obtain their
configuration. Shared client settings live in `config-repo/application.properties`;
Config Server's own client settings live in its local `application.properties`.

The gateway resolves `lb://cart-service`, `lb://user-service`,
`lb://product-service`, and `lb://order-service` through Eureka. Requests retain
their existing paths. Registry propagation and client caching can take roughly
90 seconds after startup before a newly registered service is routable.

View the dashboards at `http://localhost:8761`, `http://localhost:8762`, and
`http://localhost:8763`. Inspect any peer's registry with:

```sh
curl -H 'Accept: application/json' http://localhost:8761/eureka/apps
```

`init.sql` creates `auth_db`, `user_db`, `product_db`, `cart_db`, and `order_db`.
The local database username is `postgres` and password is `password`.
Apply `database/product.sql` to `product_db` once with the commands below to create
the `product` and `inventory` tables. Table definitions live outside the services;
Product JPA mappings validate the schema instead of creating or altering it.
PostgreSQL data persists in the `postgres-data` volume. Initialization SQL runs
only when that volume is empty; changing the SQL does not update existing data.

For a fresh database or an existing volume whose `product_db` does not yet contain
these tables, apply the schema once from this directory before starting the stack:

```sh
docker compose up -d database
docker compose exec -T database psql -U postgres -d product_db -v ON_ERROR_STOP=1 < database/product.sql
```

This script creates new tables; it does not migrate existing tables. Keep future
schema changes in separate SQL migrations instead of resetting the data volume.

Check Config Server at `http://localhost:8080/auth-service/database` and application
health at `http://localhost:8086/actuator/health` (gateway) or on each service's own port (8081–8085).

Use `docker compose logs` for logs and `docker compose down` to stop the stack
while retaining database data.

## Run locally without Docker

Build all modules from this directory with:

```sh
mvn install
```

Auth, user, cart, and order services can start without a database or Config
Server. Product service always requires a database:

- Config Server is optional. The services try `http://localhost:8888` and continue
  if it is unavailable. Set `CONFIG_SERVER_URL` to use another address. Connection
  warnings are expected when the server is not running.
- For auth, user, cart, and order, database auto-configuration is disabled unless
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

The API gateway (port 8086 when run locally) is the exception: its routes are served by
Config Server, so start Config Server (port 8888) first. Without it the gateway starts with
no routes and answers 404 to everything. It also finds the services through Eureka (see
below), so they must be registered there too.

These are Spring runtime settings, not Maven profiles. Tests run with the
`database` profile will also require a configured, reachable database.

Eureka requires Config Server. For a local registry, start Config Server on its
default port 8888, then run Eureka on 8761:

```sh
java -jar eurekaserver/target/eurekaserver-0.0.1-SNAPSHOT.jar
```

For a single local registry, pass
`--eureka.client.register-with-eureka=false --eureka.client.fetch-registry=false`
to Eureka. For three local peers, run three copies with ports 8761, 8762, and 8763
and set each peer's `--eureka.client.serviceUrl.defaultZone` to the other two
localhost URLs. Local clients default to all three localhost ports; set
`EUREKA_SERVICE_URL` to override the list. The Compose peer profiles use Docker
hostnames and are intended for running inside Compose.

The gateway needs Config Server and a running registry to route requests. Local
service instances must advertise addresses reachable from the gateway; run the
gateway and its backends together on the host or together in Compose.

## API gateway

The API gateway on `http://localhost:8086` is the single entry point for clients. It only
decides where a request goes, by path, and forwards it unchanged, including the
`Authorization` header. It does not validate tokens or request contents: every service
still checks the bearer token and the request itself, and the services remain reachable
on their own ports (8081–8085) for debugging.

| Gateway path | Service |
| --- | --- |
| `/auth/**` | auth-service |
| `/certainlyinstock/users/**` | user-service |
| `/certainlyinstock/product-service/products/**` | product-service |
| `/users/{id}/cart/**` | cart-service |
| `/api/payments/**` | order-service (payments) |

Paths are not rewritten, so a gateway URL is the same as the service's own URL with a
different host and port. Tokens are not issued by the gateway: get them from Keycloak at
`http://localhost:8180` (see below).

Routes live in Config Server: `config-repo/api-gateway.properties`, repeated in
`config-repo/api-gateway-docker.properties` for the `docker` profile used in Compose. Each
route targets `lb://<service-name>`, so the gateway looks the service up in Eureka and
load-balances across its registered instances. Spring replaces a list as a whole when it
is defined in two files, so the docker file repeats every route: change both files
together; a unit test fails if they drift apart. `/actuator/gateway/routes` lists the
loaded routes in Compose (development only).

## Authentication (Keycloak)

Keycloak handles login, issues JWT access tokens and stores passwords; no service sees
or stores a password. Every application service checks the bearer token on each request
(signature, expiry and issuer) and then applies role rules. Tokens carry Keycloak realm
roles; each service maps them to Spring roles (`ADMIN` becomes `ROLE_ADMIN`).

Keycloak imports the `certainlyinstock` realm from `keycloak/realm-export.json` when its
container starts. Its data lives inside the container, so removing the container resets
the realm to that file. The admin console is at `http://localhost:8180` (`admin` / `admin`).

Test users (development only):

| User | Password | Roles |
| --- | --- | --- |
| `admin@certainlyinstock.local` | `admin1234` | ADMIN, CUSTOMER |
| `customer@certainlyinstock.local` | `customer1234` | CUSTOMER |

### Get a token and call a service

The `certainlyinstock-api` client accepts a username and password directly. It exists for
Postman and curl only; a browser frontend should use the `certainlyinstock-web` client
(authorization code with PKCE) instead.

```sh
TOKEN=$(curl -s http://localhost:8180/realms/certainlyinstock/protocol/openid-connect/token   -d grant_type=password -d client_id=certainlyinstock-api   -d username=customer@certainlyinstock.local -d password=customer1234   | python -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

curl -H "Authorization: Bearer $TOKEN" http://localhost:8086/auth/me
```

Access tokens last 5 minutes. Use the `refresh_token` from the same response with
`grant_type=refresh_token` for a new one (valid for 30 minutes of inactivity), and post
it to `/realms/certainlyinstock/protocol/openid-connect/logout` to log out.

### auth-service endpoints

Base URL `http://localhost:8086/auth` (through the gateway; auth-service itself is on
`http://localhost:8081/auth`).

| Method | Path | Token | Result |
| --- | --- | --- | --- |
| POST | `/register` | none | Creates the Keycloak user (role CUSTOMER) and the user-service profile; 201 |
| GET | `/me` | any | The caller's Keycloak id, email, names and roles |
| POST | `/password-reset` | none | Always 202 with no body; emails a reset link if the account exists |

Register body: `{"email", "password", "firstName", "lastName"}` (password at least 8
characters). A duplicate email returns 409 and invalid input returns 400. Registration
spans two systems: if creating the profile fails, auth-service deletes the Keycloak user
again so the two do not drift apart. The profile's `keycloakId` equals the user's token
`sub` claim.

Password reset body: `{"email"}`. The response is identical whether or not the account
exists, so the endpoint cannot reveal who is registered. One email per address per 60
seconds. In development the email goes to Mailpit: open `http://localhost:8025`, follow
the link, and set a new password on Keycloak's page. Links expire after 30 minutes and
work once. Keycloak's own "Forgot password?" link on its login page does the same.

### Who can call what

| Service | Rule |
| --- | --- |
| auth-service | `/register` and `/password-reset` are public; everything else needs a token |
| user-service | `create`: ADMIN or SERVICE (auth-service's account); all other endpoints: ADMIN |
| product-service | `GET` is public; create, update and delete: ADMIN |
| cart-service | CUSTOMER or ADMIN |
| payment / order service | CUSTOMER or ADMIN; list all, update and delete payments: ADMIN |

`/actuator/health` is public on every service. A missing or invalid token returns 401; a
valid token without the required role returns 403. Owner checks (a customer may only
see their own cart or payments) are not implemented yet.

### Postman

`../postman/CertainlyInStock-Auth.postman_collection.json` covers tokens, registration,
`/me`, password reset and the 401/403/200 rules for every service, with test assertions.
Requests go through the gateway (`gatewayUrl`, `http://localhost:8086`); tokens come straight
from Keycloak (`keycloakUrl`). Run the folders in order (folder 1 stores the tokens), or from
the command line:

```sh
npx newman run ../postman/CertainlyInStock-Auth.postman_collection.json
```

### Configuration

Keycloak settings are served by Config Server (`config-repo/application.properties` and
`config-repo/auth-service.properties`). Tokens always name `http://localhost:8180` as
their issuer, while containers download Keycloak's signing keys from
`http://keycloak:8080`; `docker-compose.yml` sets `KEYCLOAK_JWK_SET_URI`,
`KEYCLOAK_BASE_URL` and `USER_SERVICE_URL` for the containers. These placeholders are
written as `\${...}` in `config-repo` so each service, not Config Server, resolves them.
`config-repo` is packaged into the Config Server image: rebuild it
(`docker compose build configserver`) after changing those files.

The `auth-service` client secret defaults to the development value in the realm file;
set `KEYCLOAK_CLIENT_SECRET` (and change it in Keycloak) outside development.

### Troubleshooting

- **A correct password returns "Invalid user credentials".** Keycloak locks an account
  for 60 seconds after failed logins less than one second apart. Wait a minute, or clear
  it in the admin console (Users, select the user, then disable the temporary lock).
- **Every request returns 401 with a valid-looking token.** Check the token's `iss` claim is
  `http://localhost:8180/realms/certainlyinstock`, and that the service can reach
  `KEYCLOAK_JWK_SET_URI`.
- **Tests do not need Keycloak.** They use mocked tokens and a placeholder key URL.

## User CRUD

User endpoints live in user-service (host port 8082 in Compose), so the base URL is
`http://localhost:8086/certainlyinstock/users` through the gateway, or
`http://localhost:8082/certainlyinstock/users` directly. 

| Method | Path | Result |
| --- | --- | --- |
| POST | `/create` | Create; returns 201 with the user |
| GET | `/getAll` | List users |
| GET | `/credential/{id}` | Read one user |
| PUT | `/updateCredentials/{id}` | Replace its writable fields |
| DELETE | `/delete/{id}` | Delete; returns 204 |

The controller accepts the User entity directly; no request DTO is used.
Example POST or PUT body:

```json
{
  "email": "jane@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "CUSTOMER"
}
```

`role` must be a value of the `Role` enum. Responses also contain the generated
`id` (`Long`), `createdAt`, and `updatedAt` as ISO instants. Creation sets both
timestamps; updates preserve `createdAt` and refresh `updatedAt`. PUT replaces
`email`, `firstName`, `lastName`, and `role`; any ID or timestamps in the body are
ignored, and PUT uses the ID in the URL.

Errors return a JSON body with `timestamp`, `status`, `error`, and `message`.
Missing user IDs return 404. `email` is unique: creating or updating a user to an
email that another user already has returns 409.

## Product CRUD

Product endpoints are always enabled. The base URL in Compose is
`http://localhost:8086/certainlyinstock/product-service` through the gateway, or
`http://localhost:8083/certainlyinstock/product-service` directly. Business endpoints use
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
