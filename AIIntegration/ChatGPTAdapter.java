package AIIntegration;

import java.io.IOException;
import java.util.Objects;

public class ChatGPTAdapter implements AIService {
    private final ChatGPT5API adaptee;

    public ChatGPTAdapter(ChatGPT5API adaptee) {
        this.adaptee = Objects.requireNonNull(adaptee, "ChatGPT API is required.");
    }

    @Override
    public String generateResponse(String prompt) throws IOException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("AI prompt is required.");
        }
        ChatGPT5Response response = adaptee.chatCompletion(prompt);
        if (response == null || response.getContent() == null
                || response.getContent().trim().isEmpty()) {
            throw new IOException("The AI service returned an empty response.");
        }
        return adaptResponse(response.getContent());
    }

    public String adaptResponse(String rawContent) throws IOException {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new IOException("The AI service returned an empty response.");
        }
        return rawContent.trim();
    }
}
