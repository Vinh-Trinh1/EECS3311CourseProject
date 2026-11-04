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

## Knock email verification

The application uses the [Knock Java SDK](https://github.com/knocklabs/knock-java)
to trigger email verification workflows. Set `KNOCK_API_KEY` and
`KNOCK_2FA_WORKFLOW_KEY` in the environment before launching the application.
Configure the workflow with an email step addressed to the recipient and include
`{{ data.code }}` in the message. The application identifies the recipient by
the account's email address and sends the generated verification code as
`data.code`.
