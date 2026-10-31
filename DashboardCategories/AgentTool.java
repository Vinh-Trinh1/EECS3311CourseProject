package DashboardCategories;

import java.sql.SQLException;

public interface AgentTool {
    String getName();

    String execute() throws SQLException;
}
