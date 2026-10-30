# Subly

## Requirements

- Java 21
- Docker, or your own MySQL server

## Running locally

### With Docker

Start MySQL:

```bash
docker compose up -d
```

Run the app:

```bash
./mvnw spring-boot:run
```

The app runs at http://localhost:8080.

Stop MySQL with `docker compose down`. The data is kept in a Docker volume.

### With your own MySQL

Create a database and user:

```sql
CREATE DATABASE subly;
CREATE USER 'subly'@'localhost' IDENTIFIED BY 'subly';
GRANT ALL PRIVILEGES ON subly.* TO 'subly'@'localhost';
```

Then run the app with `./mvnw spring-boot:run`.

## Configuration

The database connection is set with environment variables:

| Variable      | Default                              |
|---------------|--------------------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/subly`  |
| `DB_USERNAME` | `subly`                              |
| `DB_PASSWORD` | `subly`                              |

To use a different database or credentials, set the variables before running:

```bash
DB_URL=jdbc:mysql://myhost:3306/subly DB_USERNAME=me DB_PASSWORD=secret ./mvnw spring-boot:run
```
