package AIChat;

import AIIntegration.AIService;
import java.io.IOException;
import java.sql.SQLException;
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
        String context = toolRegistry.execute("financial_data");
        return run(question, context);
    }
}
