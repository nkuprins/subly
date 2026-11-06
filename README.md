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

### Deploying with Docker on Render

Create a Render Web Service from this repository and choose **Docker** as the runtime.
Render will build the included `Dockerfile` and provide the app's listening port through
the `PORT` environment variable. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
as Render environment variables using a managed MySQL-compatible database.

### With your own MySQL

Create a database and user:

```sql
CREATE DATABASE subly;
CREATE USER 'subly'@'localhost' IDENTIFIED BY 'subly';
GRANT ALL PRIVILEGES ON subly.* TO 'subly'@'localhost';
```

Then run the app. The default URL uses port 3307 (the Docker port), so point it to your server's port, usually 3306:

```bash
DB_URL=jdbc:mysql://localhost:3306/subly ./mvnw spring-boot:run
```

## Configuration

The database connection is set with environment variables:

| Variable      | Default                              |
|---------------|--------------------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3307/subly`  |
| `DB_USERNAME` | `subly`                              |
| `DB_PASSWORD` | `subly`                              |

To use a different database or credentials, set the variables before running:

```bash
DB_URL=jdbc:mysql://myhost:3306/subly DB_USERNAME=me DB_PASSWORD=secret ./mvnw spring-boot:run
```

## Code style

Java code is formatted with [Spotless](https://github.com/diffplug/spotless) and
[palantir-java-format](https://github.com/palantir/palantir-java-format). CI fails if the code is not formatted.

Format the code before you push:

```bash
./mvnw spotless:apply
```

Check the format without changing files:

```bash
./mvnw spotless:check
```
