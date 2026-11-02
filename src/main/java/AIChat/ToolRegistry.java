package AIChat;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ToolRegistry {
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public void register(AgentTool tool) {
        Objects.requireNonNull(tool, "Tool is required.");
        if (tools.putIfAbsent(tool.getName(), tool) != null) {
            throw new IllegalArgumentException("A tool with this name is already registered.");
        }
    }

    public String execute(String toolName) throws SQLException {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown AI tool: " + toolName);
        }
        return tool.execute();
    }
}
