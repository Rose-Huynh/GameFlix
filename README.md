# GameFlix Prototype

Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, and MySQL.

## Setup

Use JDK 21 and a running MySQL server on port 3306.

In MySQL Workbench, create the database:

```sql
CREATE DATABASE IF NOT EXISTS gameflix;
```

Copy `application.properties.example` to the ignored root file
`application-local.properties`. Use your existing local MySQL account, or create
a dedicated account with access to `gameflix`. Replace the copied file's database
credentials and signing-key placeholders with these settings for the root setup;
keep its lifetime and front-end origin settings:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gameflix
spring.datasource.username=root
spring.datasource.password=${MYSQL_PASSWORD}
app.jwt.secret=${JWT_SECRET}
```

Set `MYSQL_PASSWORD` to your MySQL root password and `JWT_SECRET` to the output
of `openssl rand -base64 32`, without surrounding quotes or extra whitespace.
In IntelliJ, put them in the GameFlix run configuration's environment variables,
set its working directory to the project root, and run `GameFlixApplication`.

For terminal use, set/export those variables in that terminal separately;
IntelliJ run-configuration variables do not carry over. Then run from the project
root with JDK 21 selected:

```sh
./mvnw spring-boot:run
```

Never commit real credentials or keys. A successful start logs
`Started GameFlixApplication` without database connection or schema errors.

The API runs at `http://localhost:8080`; there is no homepage. MySQL stores
application data. `ddl-auto=update` preserves
users between runs. Existing schema changes may require a manual migration.

## Endpoints

Testing this API confirms that registration stores password hashes, login accepts
valid credentials and rejects incorrect ones, and protected profiles require a
valid token. Testing both success and failure cases helps catch security issues
and verifies that each endpoint returns the expected response.

Register and log in with JSON:

```json
{"email":"assignment1@example.com","password":"password123!"}
```

| Endpoint | Responses |
|---|---|
| POST `/api/auth/register` | 201 created; 400 invalid input; 409 duplicate email |
| POST `/api/auth/login` | 200 with JWT; 401 invalid credentials |
| GET `/api/users/me` | 200 with caller's ID and email; 401 missing, invalid, or expired token |

In Postman, use the login response's `token` under Authorization → Bearer Token
for `/api/users/me`; this GET request has no body. Save requests in a collection
and export it with a `{{token}}` placeholder instead of a live token.

## Security Choices

BCrypt stores passwords as salted hashes. Strength 12 makes guessing harder than
the minimum strength of 10 while keeping login practical. Hashes use a `{bcrypt}`
prefix; passwords are limited to 64 characters and 72 bytes to avoid truncation.

Signed JWTs expire after 15 minutes, limiting stolen-token use. The signing key
stays outside GitHub. The server checks signatures, expiration, issuer, and user
ID. Tokens contain no confidential data; their contents are readable, not encrypted.

Login errors use one generic message. CORS allows only the configured front-end
origin, initially `http://localhost:3000`. Protected requests require a bearer token.

## Database Structure and Updates

The original architecture planned User, Catalog, and Subscription services with
separate databases and an external payment service. This prototype implements
the user-service portion in one Spring Boot application backed by MySQL;
the front end, catalog, subscriptions, and payments remain future work.

The reference schema is in `src/main/resources/db/schema.sql`. Columns formerly
named `user_id`, `user_email`, and `password_hash` are now `id`, `email`, and
`password`; optional `user_name` remains. Authentication uses normalized email.
The supplied architecture diagram does not specify table columns; compare these
column changes with the separate Module 3 ERD before submission.

`password` is `VARCHAR(255)` and stores only hashes. The database constraint
`uk_users_email` enforces uniqueness; its violation produces HTTP 409.

```sql
SELECT id, email, password FROM gameflix.users;
```

## Test evidence

Testing is performed with Postman. Submit the exported collection and a separate
Word document containing endpoint success/failure screenshots and MySQL hash-storage evidence.

## AI assistance

OpenAI Codex assisted with implementation, configuration, and documentation.
All AI-assisted code and documentation are reviewed before submission to ensure
they are understood and meet the project requirements.
