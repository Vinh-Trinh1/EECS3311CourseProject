package DashboardCategories;

import java.io.IOException;
import java.util.Locale;

public class OfflineChatGPT5 implements ChatGPT5API {
    @Override
    public ChatGPT5Response chatCompletion(String prompt) throws IOException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("AI prompt is required.");
        }
        String normalized = prompt.toLowerCase(Locale.ROOT);
        if (normalized.contains("categorize expense:")) {
            return new ChatGPT5Response(categorize(normalized));
        }
        if (normalized.contains("budget recommendation for")) {
            return new ChatGPT5Response(
                    "This offline demo budget uses your recent recorded spending as a baseline "
                            + "and trims each category slightly to leave room for savings. "
                            + "Review the category limits against upcoming bills before relying on them.");
        }
        String context = extractContext(prompt);
        if (context != null) {
            return new ChatGPT5Response(
                    "Based on the financial records available to this offline demo: "
                            + context
                            + " These records are historical and may not include income or pending bills.");
        }
        return new ChatGPT5Response(
                "This is the offline finance assistant demo. Ask about your recorded expenses, "
                        + "account balance, savings goals, or request a budget recommendation.");
    }

    private static String categorize(String prompt) {
        int descriptionStart = prompt.indexOf("categorize expense:") + "categorize expense:".length();
        int allowedValuesStart = prompt.indexOf(". return one category name only from", descriptionStart);
        String description = prompt.substring(
                descriptionStart,
                allowedValuesStart < 0 ? prompt.length() : allowedValuesStart).trim();
        if (containsAny(description, "grocery", "groceries", "supermarket", "produce", "food")) {
            return "Groceries";
        }
        if (containsAny(description, "rent", "mortgage", "housing")) {
            return "Housing";
        }
        if (containsAny(description, "gas", "fuel", "transit", "bus", "train", "uber")) {
            return "Transportation";
        }
        if (containsAny(description, "doctor", "pharmacy", "medical", "health")) {
            return "Healthcare";
        }
        if (containsAny(description, "electric", "internet", "utility", "phone bill")) {
            return "Bills";
        }
        if (containsAny(description, "movie", "concert", "game", "restaurant", "entertainment")) {
            return "Entertainment";
        }
        if (containsAny(description, "car payment", "auto loan")) {
            return "Car payment";
        }
        return "Other";
    }

    private static String extractContext(String prompt) {
        String marker = "Financial context: ";
        int start = prompt.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = prompt.indexOf("\nQuestion:", start);
        return prompt.substring(start, end < 0 ? prompt.length() : end).trim();
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
