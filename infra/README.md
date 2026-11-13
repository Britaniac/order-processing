Start the order-service database from the project root:

```shell
docker compose -f infra/compose.yaml up -d order-db
./gradlew :services:order-service:bootRun --args='--spring.profiles.active=local'
```

The PostgreSQL image creates the `orders` database and `order_service` role
using `POSTGRES_DB` and `POSTGRES_USER` on the first startup with an empty volume.
Flyway creates the tables when order-service starts; Hibernate validates the schema.
Database initialization settings do not alter an existing database volume.

Local defaults are `localhost:5432/orders`, username `order_service`, and password
`order_service`, configured in `application-local.yml`. Activate the `local` profile
with `--spring.profiles.active=local` or the `SPRING_PROFILES_ACTIVE=local` environment
variable (also suitable for an IntelliJ run configuration).
Override the password with `ORDER_DB_PASSWORD` in both the Compose
and application environments. The application also accepts `ORDER_DB_URL` and
`ORDER_DB_USERNAME`. Without the `local` profile, all three database environment
variables must be supplied. These defaults are intended for local development.

Run PostgreSQL integration tests with a running Docker engine:

```shell
./gradlew :services:order-service:test
```

Tests start an isolated PostgreSQL container, run Flyway, validate entity mappings,
and check persistence and the positive-quantity constraint.

`orders` avoids the SQL reserved word `order`. Each `order_item` has its own ID;
its `price` is the unit-price snapshot. Product IDs are external references with
no foreign key into another service's database. Money uses two decimal places.

The service layer must eventually keep totals consistent with items in a transaction
and protect concurrent order updates. The current entities do not enforce total
calculation or legal status transitions; concurrent writers can overwrite changes.
