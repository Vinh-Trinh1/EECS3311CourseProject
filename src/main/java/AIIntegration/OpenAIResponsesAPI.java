package AIIntegration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenAIResponsesAPI implements ChatGPT5API {
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/responses";
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final URL endpoint;

    public OpenAIResponsesAPI(String apiKey, String model) {
        this(apiKey, model, createDefaultEndpoint());
    }

    OpenAIResponsesAPI(String apiKey, String model, URL endpoint) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI model is required.");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("OpenAI API endpoint is required.");
        }
        this.apiKey = apiKey == null ? null : apiKey.trim();
        this.model = model.trim();
        this.endpoint = endpoint;
    }

    @Override
    public ChatGPT5Response chatCompletion(String prompt) throws IOException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("AI prompt is required.");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException(
                    "OpenAI API key is missing. Add it to config.properties or set "
                            + "the OPENAI_API_KEY environment variable.");
        }

        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");

        Map<String, String> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("input", prompt);

        try {
            byte[] requestBody = JSON.writeValueAsBytes(request);
            connection.setFixedLengthStreamingMode(requestBody.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody);
            }

            int status = connection.getResponseCode();
            String responseBody = readResponseBody(connection, status);
            if (status < 200 || status >= 300) {
                throw new IOException(formatApiError(status, responseBody));
            }
            return new ChatGPT5Response(extractText(responseBody));
        } finally {
            connection.disconnect();
        }
    }

    private static String readResponseBody(HttpURLConnection connection, int status)
            throws IOException {
        if (status >= 200 && status < 300) {
            return readBody(connection.getInputStream());
        }
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            return readBody(errorStream);
        }
        try {
            return readBody(connection.getInputStream());
        } catch (IOException exception) {
            InputStream fallbackStream = connection.getErrorStream();
            return fallbackStream == null ? "" : readBody(fallbackStream);
        }
    }

    private static URL createDefaultEndpoint() {
        try {
            return new URL(DEFAULT_ENDPOINT);
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalStateException("Invalid OpenAI API endpoint.", exception);
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                body.append(buffer, 0, count);
            }
        }
        return body.toString();
    }

    private static String extractText(String responseBody) throws IOException {
        JsonNode response = JSON.readTree(responseBody);
        if (response == null) {
            throw new IOException("OpenAI returned an empty response.");
        }
        JsonNode output = response.path("output");
        StringBuilder text = new StringBuilder();
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText())
                            && part.path("text").isTextual()) {
                        if (text.length() > 0) {
                            text.append('\n');
                        }
                        text.append(part.path("text").asText());
                    }
                }
            }
        }
        if (text.toString().trim().isEmpty()) {
            throw new IOException("OpenAI returned no text in its response.");
        }
        return text.toString();
    }

    private static String formatApiError(int status, String responseBody) {
        String message = "OpenAI API request failed with HTTP " + status + ".";
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return message;
        }
        try {
            JsonNode error = JSON.readTree(responseBody).path("error").path("message");
            if (error.isTextual() && !error.asText().trim().isEmpty()) {
                return message + " " + error.asText();
            }
        } catch (JsonProcessingException exception) {
            return message + " The error response could not be parsed.";
        }
        return message;
    }
}
