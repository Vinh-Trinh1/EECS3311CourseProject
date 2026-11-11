package AIIntegration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OpenAIResponsesAPITest {
    @Test
    public void sendsResponsesRequestAndExtractsOutputText() throws Exception {
        final AtomicReference<String> authorization = new AtomicReference<>();
        final AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(readBody(exchange.getRequestBody()));
            respond(exchange, 200,
                    "{\"output\":[{\"type\":\"message\",\"content\":["
                            + "{\"type\":\"output_text\",\"text\":\"Hello from OpenAI\"}]}]}");
        });
        server.start();
        try {
            URL endpoint = new URL("http://127.0.0.1:"
                    + server.getAddress().getPort() + "/v1/responses");
            ChatGPT5Response response =
                    new OpenAIResponsesAPI("test-key", "test-model", endpoint)
                            .chatCompletion("Test prompt");

            assertEquals("Hello from OpenAI", response.getContent());
            assertEquals("Bearer test-key", authorization.get());
            JsonNode request = new ObjectMapper().readTree(requestBody.get());
            assertEquals("test-model", request.path("model").asText());
            assertEquals("Test prompt", request.path("input").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void reportsProviderErrorMessage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            readBody(exchange.getRequestBody());
            respond(exchange, 429, "{\"error\":{\"message\":\"Rate limit exceeded\"}}");
        });
        server.start();
        try {
            URL endpoint = new URL("http://127.0.0.1:"
                    + server.getAddress().getPort() + "/v1/responses");
            try {
                new OpenAIResponsesAPI("bad-key", "test-model", endpoint)
                        .chatCompletion("Test prompt");
                fail("Expected an API error.");
            } catch (IOException expected) {
                assertTrue(expected.getMessage(),
                        expected.getMessage().contains("HTTP 429"));
                assertTrue(expected.getMessage(),
                        expected.getMessage().contains("Rate limit exceeded"));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void reportsMissingApiKeyWithoutMakingRequest() throws Exception {
        URL endpoint = new URL("http://127.0.0.1:1/v1/responses");
        try {
            new OpenAIResponsesAPI(null, "test-model", endpoint).chatCompletion("Test prompt");
            fail("Expected a missing-key error.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("OPENAI_API_KEY"));
        }
    }

    private static void respond(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static String readBody(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
