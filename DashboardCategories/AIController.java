package DashboardCategories;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AIController {
    private final FinancialDataFacade financialDataFacade;
    private final AIService aiService;
    private final Map<String, ChatAI> agents = new HashMap<>();

    public AIController(FinancialDataFacade financialDataFacade, AIService aiService) {
        this.financialDataFacade = Objects.requireNonNull(
                financialDataFacade, "Financial data facade is required.");
        this.aiService = Objects.requireNonNull(aiService, "AI service is required.");
    }

    public synchronized String askQuestion(String accountEmail, String question)
            throws IOException, SQLException {
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        String owner = accountEmail.trim();
        ChatAI agent = agents.get(owner.toLowerCase(java.util.Locale.ROOT));
        if (agent == null) {
            ToolRegistry registry = new ToolRegistry();
            registry.register(new GetFinancialDataTool(financialDataFacade, owner));
            agent = new ChatAI(aiService, new AgentMemory(), registry);
            agents.put(owner.toLowerCase(java.util.Locale.ROOT), agent);
        }
        return agent.processQuestion(question);
    }
}
