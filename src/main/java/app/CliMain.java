package app;

import AIChat.AIController;
import AIIntegration.AIService;
import AIIntegration.AIServiceFactory;
import AccountCreation.AccountController;
import AccountCreation.User;
import DatabaseService.DatabaseService;
import FinancialData.FinancialDataFacade;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;

public final class CliMain {
    private CliMain() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        return run(args, System.in, System.out, System.err, null);
    }

    static int run(
            String[] args,
            InputStream input,
            PrintStream output,
            PrintStream error,
            AIService aiService) {
        if (args == null || args.length == 0 || isHelpRequest(args[0])) {
            printHelp();
            return 0;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "gui":
            case "--gui":
                Main.launchGui(new String[0]);
                return 0;
            case "assistant":
                return runAssistantCommand(
                        java.util.Arrays.copyOfRange(args, 1, args.length),
                        input,
                        output,
                        error,
                        aiService);
            case "account":
                return runAccountCommand(java.util.Arrays.copyOfRange(args, 1, args.length));
            default:
                System.err.println("Unknown command: " + args[0]);
                printHelp();
                return 1;
        }
    }

    private static int runAssistantCommand(
            String[] args,
            InputStream input,
            PrintStream output,
            PrintStream error,
            AIService aiService) {
        if (args == null || args.length == 0 || !"chat".equalsIgnoreCase(args[0])) {
            printAssistantHelp(error);
            return 1;
        }

        try {
            return runAssistantChat(args, input, output, aiService);
        } catch (IllegalArgumentException | IllegalStateException
                | IOException | SQLException exception) {
            error.println("Assistant request failed: " + exception.getMessage());
            return 1;
        }
    }

    private static int runAssistantChat(
            String[] args, InputStream input, PrintStream output, AIService aiService)
            throws IOException, SQLException {
        String requestedEmail = getRequiredOption(args, "--email");
        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            String accountEmail = databaseService.findUserEmail(requestedEmail);
            if (accountEmail == null) {
                throw new IllegalArgumentException("No account found for " + requestedEmail + ".");
            }

            AIService service = aiService == null
                    ? AIServiceFactory.createDefaultService()
                    : aiService;
            AIController controller = new AIController(
                    new FinancialDataFacade(databaseService), service);
            AssistantCli assistantCli = new AssistantCli(accountEmail, controller);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            return assistantCli.run(reader, output);
        } finally {
            databaseService.close();
        }
    }

    private static int runAccountCommand(String[] args) {
        if (args == null || args.length == 0) {
            printAccountHelp();
            return 1;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "create":
                    createAccount(args);
                    return 0;
                case "login":
                    requestLoginCode(args);
                    return 0;
                case "verify":
                    verifyLoginCode(args);
                    return 0;
                case "status":
                    verifyCredentials(args);
                    return 0;
                default:
                    throw new IllegalArgumentException("Unknown account command: " + action);
            }
        } catch (IllegalArgumentException | SQLException | IOException exception) {
            System.err.println(exception.getMessage());
            printAccountHelp();
            return 1;
        }
    }

    private static void createAccount(String[] args) throws SQLException {
        String email = getRequiredOption(args, "--email");
        String phone = getRequiredOption(args, "--phone");
        String password = getRequiredOption(args, "--password");
        String contact = getOptionalOption(args, "--contact", "email");

        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            AccountController controller = new AccountController(databaseService);
            controller.createAccount(new User(email, phone, password, password, contact));
            System.out.println("Account created successfully for " + email);
        } finally {
            databaseService.close();
        }
    }

    private static void requestLoginCode(String[] args) throws SQLException, IOException {
        String identifier = getRequiredOption(args, "--identifier");
        String password = getRequiredOption(args, "--password");

        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            AccountController controller = new AccountController(databaseService);
            String email = controller.requestLoginCode(identifier, password.toCharArray());
            if (email == null) {
                System.out.println("Authentication failed. Check the identifier and password.");
                return;
            }
            System.out.println("Login code sent to " + email + ".");
        } finally {
            databaseService.close();
        }
    }

    private static void verifyLoginCode(String[] args) throws SQLException {
        String identifier = getRequiredOption(args, "--identifier");
        String code = getRequiredOption(args, "--code");

        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            AccountController controller = new AccountController(databaseService);
            boolean isValid = controller.verifyLoginCode(identifier, code);
            System.out.println(isValid ? "Verification succeeded." : "Verification failed.");
        } finally {
            databaseService.close();
        }
    }

    private static void verifyCredentials(String[] args) throws SQLException {
        String identifier = getRequiredOption(args, "--identifier");
        String password = getRequiredOption(args, "--password");

        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            AccountController controller = new AccountController(databaseService);
            boolean isValid = controller.verifyCredentials(identifier, password.toCharArray());
            System.out.println(isValid ? "Credentials valid." : "Credentials invalid.");
        } finally {
            databaseService.close();
        }
    }

    private static String getRequiredOption(String[] args, String option) {
        String value = getOptionValue(args, option);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required option: " + option);
        }
        return value;
    }

    private static String getOptionalOption(String[] args, String option, String defaultValue) {
        String value = getOptionValue(args, option);
        return value == null ? defaultValue : value;
    }

    private static String getOptionValue(String[] args, String option) {
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            if (arg.equalsIgnoreCase(option)) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for " + option);
                }
                return args[index + 1];
            }
            if (arg.startsWith(option + "=")) {
                return arg.substring(option.length() + 1);
            }
        }
        return null;
    }

    private static boolean isHelpRequest(String command) {
        return "--help".equalsIgnoreCase(command)
                || "-h".equalsIgnoreCase(command)
                || "help".equalsIgnoreCase(command);
    }

    private static void printHelp() {
        System.out.println("Usage: java -cp target/classes app.Main [COMMAND]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  gui                     Launch the Swing login GUI.");
        System.out.println("  assistant chat --email <account-email>");
        System.out.println("                          Chat with the finance assistant.");
        System.out.println("  account create --email <email> --phone <phone> --password <password> [--contact email|phone]");
        System.out.println("  account login --identifier <email-or-phone> --password <password>");
        System.out.println("  account verify --identifier <email-or-phone> --code <6-digit-code>");
        System.out.println("  account status --identifier <email-or-phone> --password <password>");
        System.out.println("  --help                  Show this help text.");
    }

    private static void printAccountHelp() {
        System.err.println("Usage: app.Main account <create|login|verify|status> [options]");
        System.err.println("Examples:");
        System.err.println("  app.Main account create --email name@example.com --phone +15551234567 --password Secret123 --contact email");
        System.err.println("  app.Main account login --identifier name@example.com --password Secret123");
        System.err.println("  app.Main account verify --identifier name@example.com --code 123456");
    }

    private static void printAssistantHelp(PrintStream output) {
        output.println("Usage: app.Main assistant chat --email <account-email>");
        output.println("Ask questions about recorded finances. Type 'exit' or 'quit' to leave.");
    }
}
