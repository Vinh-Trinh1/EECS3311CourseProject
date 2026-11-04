package AccountCreation;

import app.knock.api.client.KnockClient;
import app.knock.api.client.okhttp.KnockOkHttpClient;
import app.knock.api.core.JsonValue;
import app.knock.api.errors.KnockException;
import app.knock.api.models.users.InlineIdentifyUserRequest;
import app.knock.api.models.workflows.WorkflowTriggerParams;
import java.io.IOException;

public class KnockEmailCodeDelivery implements EmailCodeDelivery {
    private final String apiKey;
    private final String workflowKey;
    private volatile KnockClient client;

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

        try {
            getClient().workflows().trigger(WorkflowTriggerParams.builder()
                    .key(workflowKey.trim())
                    .addRecipient(InlineIdentifyUserRequest.builder()
                            .id(email.trim())
                            .email(email.trim())
                            .build())
                    .data(WorkflowTriggerParams.Data.builder()
                            .putAdditionalProperty("code", JsonValue.from(code))
                            .build())
                    .build());
        } catch (KnockException exception) {
            throw new IOException(
                    "Knock email delivery failed: " + exception.getMessage(), exception);
        }
    }

    private KnockClient getClient() {
        KnockClient currentClient = client;
        if (currentClient == null) {
            synchronized (this) {
                currentClient = client;
                if (currentClient == null) {
                    currentClient = KnockOkHttpClient.builder().apiKey(apiKey.trim()).build();
                    client = currentClient;
                }
            }
        }
        return currentClient;
    }
}
