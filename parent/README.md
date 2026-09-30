# Certainly In Stock

Build all modules from this directory with:

```sh
mvn install
```

The five service modules currently start and run their context tests without a
database or Config Server:

- Config Server is optional. The services try `http://localhost:8888` and continue
  if it is unavailable. Set `CONFIG_SERVER_URL` to use another address. Connection
  warnings are expected until the server is implemented.
- Database auto-configuration is disabled unless the Spring profile `database`
  is active. The JPA and PostgreSQL dependencies remain available.

When a database is ready, set `SPRING_PROFILES_ACTIVE=database` and provide
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` for each service. For example:

```sh
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=database
```

The datasource settings can also come from Config Server once it is implemented.
To require Config Server at that point, override `spring.config.import` with
`configserver:http://localhost:8888` (without `optional:`).

These are Spring runtime settings, not Maven profiles. Tests run with the
`database` profile will also require a configured, reachable database.
