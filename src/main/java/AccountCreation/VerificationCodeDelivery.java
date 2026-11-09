package AccountCreation;

import java.io.IOException;

public interface VerificationCodeDelivery {
    void deliver(String recipient, String code) throws IOException;
}
