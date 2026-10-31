package DashboardCategories;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

public class ChatAI extends FinanceAgent {
    private final ToolRegistry toolRegistry;

    public ChatAI(AIService aiService, AgentMemory memory, ToolRegistry toolRegistry) {
        super(aiService, memory);
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "Tool registry is required.");
    }

    public String processQuestion(String question) throws IOException, SQLException {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a question.");
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        String context = null;
        if (isFinancialQuestion(normalized)) {
            context = toolRegistry.execute("financial_data");
        }
        return run(question, context);
    }

    private static boolean isFinancialQuestion(String question) {
        return question.contains("balance")
                || question.contains("expense")
                || question.contains("spend")
                || question.contains("budget")
                || question.contains("saving")
                || question.contains("goal")
                || question.contains("financial")
                || question.contains("money");
    }
}
