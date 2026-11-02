package AccountCreation;

import java.io.IOException;

public interface TwoFactorService {
    void requestCode(String email) throws IOException;

    boolean verifyCode(String email, String code);
}
