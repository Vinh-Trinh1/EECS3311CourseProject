package app;

import AIChat.AIController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Objects;

final class AssistantCli {
    private final String accountEmail;
    private final AIController controller;

    AssistantCli(String accountEmail, AIController controller) {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        this.accountEmail = accountEmail.trim();
        this.controller = Objects.requireNonNull(controller, "AI controller is required.");
    }

    int run(BufferedReader input, PrintStream output) throws IOException, SQLException {
        Objects.requireNonNull(input, "Input is required.");
        Objects.requireNonNull(output, "Output is required.");

        output.println("Finance assistant for " + accountEmail + ".");
        output.println(
                "Ask about recorded expenses, balance, savings goals, budgets, "
                        + "or budget comparisons.");
        output.println("Type 'exit' or 'quit' to end the chat.");

        while (true) {
            output.print("> ");
            output.flush();
            String line = input.readLine();
            if (line == null) {
                return 0;
            }

            String question = line.trim();
            if (question.isEmpty()) {
                continue;
            }
            if ("exit".equalsIgnoreCase(question) || "quit".equalsIgnoreCase(question)) {
                return 0;
            }

            output.println(controller.askQuestion(accountEmail, question));
        }
    }
}
