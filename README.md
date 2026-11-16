# EECS 3311 Personal Finance Assistant

## Project layout

- `src/main/java/` contains the application, grouped by feature package.
- `src/test/java/` contains the JUnit tests, using the same package structure.
- `docs/Stage1/` contains the course specification, diagrams, and design material.
- `pom.xml` defines the Java version and project dependencies.

The SQLite implementation lives in `DatabaseService.DatabaseService`; feature
controllers use it rather than owning persistence code.

## Build and launch

Install a JDK (Java 8 or newer) and Maven, then open a terminal in the project
root. Compile and run the test suite with:

```powershell
mvn clean test
```

Launch the Swing application (this is the default when no command is supplied):

```powershell
mvn exec:java
```

The application creates or opens `finance.db` in the project root. To launch the
GUI explicitly, use `mvn exec:java "-Dexec.args=gui"`.

### CLI

Use the CLI help command to see the available commands:

```powershell
mvn exec:java "-Dexec.args=--help"
```

Example account commands:

```powershell
mvn exec:java "-Dexec.args=account create --email name@example.com --phone +15551234567 --password Secret123 --contact email"
mvn exec:java "-Dexec.args=account login --identifier name@example.com --password Secret123"
mvn exec:java "-Dexec.args=account verify --identifier name@example.com --code 123456"
```

Start an interactive CLI session with the finance assistant:

```powershell
mvn compile exec:java "-Dexec.args=assistant chat --email name@example.com"
```

Ask questions about the selected account's recorded finances, then enter
`exit` or `quit` to end the session. The assistant loads financial context for
each question and retains recent turns while the CLI session is running. Set
`OPENAI_API_KEY` or configure `config.properties` before asking questions.

The `status` command checks an email/phone and password; it does not print
account details. CLI passwords may be saved in shell history or visible to
other local processes, so prefer the GUI when that is a concern.

## OpenAI API configuration

The AI features use OpenAI's Responses API. In the repository root, copy
`config.properties.example` to `config.properties`, then set `openai.api.key`
to your API key. The local
`config.properties` is excluded by `.gitignore`; never commit or upload it.
The default model is `gpt-4.1-mini`. To choose another model available to your
API project, change `openai.model`. Environment variables `OPENAI_API_KEY` and
`OPENAI_MODEL`, when set, override the corresponding local configuration values.

In PowerShell, create the local configuration file with:

```powershell
Copy-Item config.properties.example config.properties
```

Alternatively, configure the values in a temporary PowerShell session:

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
