package AIIntegration;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AIServiceProxy implements AIService {
    private static final long RATE_WINDOW_MILLIS = 60_000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final int MAX_CACHE_ENTRIES = 100;
    private static final int MAX_PROMPT_LENGTH = 8_000;

    private final AIService realService;
    private final ArrayDeque<Long> requestTimes = new ArrayDeque<>();
    private final Map<String, String> responseCache =
            new LinkedHashMap<String, String>(MAX_CACHE_ENTRIES, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            };

    public AIServiceProxy(AIService realService) {
        this.realService = Objects.requireNonNull(realService, "AI service is required.");
    }

    @Override
    public synchronized String generateResponse(String prompt) throws IOException {
        String normalizedPrompt = checkAccess(prompt);
        String cacheKey = normalizedPrompt.toLowerCase(Locale.ROOT);
        String cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        checkRateLimit();
        String response = realService.generateResponse(normalizedPrompt);
        if (response == null || response.trim().isEmpty()) {
            throw new IOException("The AI service returned an empty response.");
        }
        responseCache.put(cacheKey, response);
        return response;
    }

    private static String checkAccess(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("AI prompt is required.");
        }
        String normalizedPrompt = prompt.trim();
        if (normalizedPrompt.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException("AI prompt exceeds the maximum supported length.");
        }
        return normalizedPrompt;
    }

    private void checkRateLimit() throws IOException {
        long now = System.currentTimeMillis();
        while (!requestTimes.isEmpty() && now - requestTimes.peekFirst() >= RATE_WINDOW_MILLIS) {
            requestTimes.removeFirst();
        }
        if (requestTimes.size() >= MAX_REQUESTS_PER_WINDOW) {
            throw new IOException("AI request limit reached. Please try again in a minute.");
        }
        requestTimes.addLast(now);
    }
}
