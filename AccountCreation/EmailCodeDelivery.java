package AccountCreation;

import java.io.IOException;

public interface EmailCodeDelivery {
    void deliver(String email, String code) throws IOException;
}
