package AIChat;

import AIIntegration.AIService;
import java.io.IOException;
import java.util.Objects;

public abstract class FinanceAgent {
    private final AIService aiService;
    private final AgentMemory memory;

    protected FinanceAgent(AIService aiService, AgentMemory memory) {
        this.aiService = Objects.requireNonNull(aiService, "AI service is required.");
        this.memory = Objects.requireNonNull(memory, "Agent memory is required.");
    }

    protected String run(String question, String financialContext) throws IOException {
        StringBuilder prompt = new StringBuilder();
        String previousTurns = memory.asPromptContext();
        if (!previousTurns.isEmpty()) {
            prompt.append("Recent conversation:\n").append(previousTurns);
        }
        if (financialContext != null && !financialContext.trim().isEmpty()) {
            prompt.append("Financial context: ").append(financialContext.trim()).append('\n');
        }
        prompt.append("Question: ").append(question.trim());
        String response = aiService.generateResponse(prompt.toString());
        memory.remember(question.trim(), response);
        return response;
    }
}
