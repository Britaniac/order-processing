# Order processing

Requires Java 25. Build and test all services with `./gradlew build` (Windows: `.\gradlew.bat build`).

Run an application with `./gradlew :services:order-service:bootRun`.
Replace `order-service` with `inventory-service` or `payment-service` as needed.

Service ports: order 8080, inventory 8081, payment 8082.

Each service is an independent Spring Boot application with no project dependencies.
Future databases and migrations must remain owned by their respective services.
The order service migration directory is reserved for future use; no migration engine
or database is configured. The Compose file is a placeholder with no services yet.
