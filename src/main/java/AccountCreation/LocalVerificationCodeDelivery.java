package AccountCreation;

import java.io.IOException;

public class LocalVerificationCodeDelivery implements VerificationCodeDelivery {
    @Override
    public void deliver(String recipient, String code) throws IOException {
        System.out.println("Development-only verification code: " + code);
    }
}
