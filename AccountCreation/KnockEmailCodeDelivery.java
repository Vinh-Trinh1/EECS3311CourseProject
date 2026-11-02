package AccountCreation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class KnockEmailCodeDelivery implements EmailCodeDelivery {
    private static final String API_BASE_URL = "https://api.knock.app/v1/workflows/";
    private static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final int READ_TIMEOUT_MILLIS = 10000;

    private final String apiKey;
    private final String workflowKey;

    public KnockEmailCodeDelivery() {
        this(System.getenv("KNOCK_API_KEY"), System.getenv("KNOCK_2FA_WORKFLOW_KEY"));
    }

    public KnockEmailCodeDelivery(String apiKey, String workflowKey) {
        this.apiKey = apiKey;
        this.workflowKey = workflowKey;
    }

    @Override
    public void deliver(String email, String code) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Set KNOCK_API_KEY to enable email verification.");
        }
        if (workflowKey == null || workflowKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Set KNOCK_2FA_WORKFLOW_KEY to enable email verification.");
        }
        if (email == null || email.trim().isEmpty() || code == null || !code.matches("\\d{6}")) {
            throw new IllegalArgumentException("A verified email and six-digit code are required.");
        }

        String encodedWorkflow = URLEncoder.encode(workflowKey.trim(), "UTF-8")
                .replace("+", "%20");
        URL url = new URL(API_BASE_URL + encodedWorkflow + "/trigger");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        String payload = "{\"recipients\":[{\"id\":\"" + escapeJson(email.trim())
                + "\",\"email\":\"" + escapeJson(email.trim())
                + "\"}],\"data\":{\"code\":\"" + code + "\"}}";
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Knock email delivery failed with HTTP status " + status + ".");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\\') {
                escaped.append('\\').append(character);
            } else if (character < 0x20) {
                escaped.append(String.format("\\u%04x", (int) character));
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
