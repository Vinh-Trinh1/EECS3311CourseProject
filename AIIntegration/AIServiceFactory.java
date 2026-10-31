package AIIntegration;

public final class AIServiceFactory {
    private AIServiceFactory() {
    }

    public static AIService createDefaultService() {
        return new AIServiceProxy(new ChatGPTAdapter(new OfflineChatGPT5()));
    }
}
