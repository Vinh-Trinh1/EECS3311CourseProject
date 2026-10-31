package DashboardCategories;

import java.sql.SQLException;
import java.util.Objects;

public class GetFinancialDataTool implements AgentTool {
    private final FinancialDataFacade financialDataFacade;
    private final String accountEmail;

    public GetFinancialDataTool(
            FinancialDataFacade financialDataFacade, String accountEmail) {
        this.financialDataFacade = Objects.requireNonNull(
                financialDataFacade, "Financial data facade is required.");
        if (accountEmail == null || accountEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Account email is required.");
        }
        this.accountEmail = accountEmail.trim();
    }

    @Override
    public String getName() {
        return "financial_data";
    }

    @Override
    public String execute() throws SQLException {
        return financialDataFacade.getFinancialData(accountEmail).toPromptSummary();
    }
}
