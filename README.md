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

## OpenAI API

The AI features use OpenAI's Responses API. In the repository root, copy
`config.properties.example` to `config.properties`, then set `openai.api.key`
to your API key. The local
`config.properties` is excluded by `.gitignore`; never commit or upload it.
The default model is `gpt-4.1-mini`. To choose another model available to your
API project, change `openai.model`. Environment variables `OPENAI_API_KEY` and
`OPENAI_MODEL`, when set, override the corresponding local configuration values.

Alternatively, you can configure the values in a temporary PowerShell session:

```powershell
$env:OPENAI_API_KEY = "your-api-key"
$env:OPENAI_MODEL = "gpt-4.1-mini"
```

API calls require API billing/credits and send the relevant prompt (which can
include financial details) to OpenAI. Keep the key private.

## Local development verification codes

The application does not use Knock or an external email/SMS service. After a
successful password check, it prints the one-time verification code to the
application console. Codes expire after five minutes, are single-use, and three
failed attempts cause a 15-minute lockout.

Console delivery is for local development only: anyone with access to the
console can read the code. Do not use this delivery method in production. The
account still requires an email address, but the code is not sent by email.
