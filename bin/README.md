# EECS 3311 Personal Finance Assistant

## Project layout

- `src/main/java/` contains the application, grouped by feature package.
- `src/test/java/` contains the JUnit tests, using the same package structure.
- `docs/Stage1/` contains the course specification, diagrams, and design material.
- `pom.xml` defines the Java version and project dependencies.

The SQLite implementation lives in `DatabaseService.DatabaseService`; feature
controllers use it rather than owning persistence code.

## Build and test

Run the full test suite with Maven:

```text
mvn test
```

The Swing application entry point is `app.Main`.

## Local development verification codes

The application does not use Knock or an external email/SMS service. After a
successful password check, it prints the one-time verification code to the
application console. Codes expire after five minutes, are single-use, and three
failed attempts cause a 15-minute lockout.

Console delivery is for local development only: anyone with access to the
console can read the code. Do not use this delivery method in production. The
account still requires an email address, but the code is not sent by email.
