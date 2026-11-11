package AIIntegration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AIServiceFactory {
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final String CONFIG_FILE = "config.properties";

    private AIServiceFactory() {
    }

    public static AIService createDefaultService() {
        Properties configuration = loadConfiguration();
        String apiKey = setting(
                "OPENAI_API_KEY", configuration.getProperty("openai.api.key"));
        String model = setting(
                "OPENAI_MODEL", configuration.getProperty("openai.model"));
        if (model == null) {
            model = DEFAULT_MODEL;
        }
        return new AIServiceProxy(new ChatGPTAdapter(
                new OpenAIResponsesAPI(apiKey, model)));
    }

    private static Properties loadConfiguration() {
        Properties configuration = new Properties();
        Path path = Paths.get(System.getProperty("user.dir"), CONFIG_FILE);
        if (!Files.exists(path)) {
            return configuration;
        }
        try (InputStream input = Files.newInputStream(path)) {
            configuration.load(input);
            return configuration;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read local OpenAI configuration at " + path + ".", exception);
        }
    }

    private static String setting(String environmentName, String fileValue) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }
        if (fileValue != null && !fileValue.trim().isEmpty()) {
            return fileValue.trim();
        }
        return null;
    }
}
