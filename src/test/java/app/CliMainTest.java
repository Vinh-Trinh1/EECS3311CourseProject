package app;

import DatabaseService.DatabaseService;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CliMainTest {
    private final String email = "cli-" + UUID.randomUUID() + "@example.com";
    private final String phone = "+1" + String.format("%010d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 1000000000L));

    @After
    public void tearDown() throws Exception {
        DatabaseService databaseService = DatabaseService.getInstance();
        try (PreparedStatement statement = databaseService.getConnection()
                .prepareStatement("DELETE FROM account_users WHERE email = ?")) {
            statement.setString(1, email);
            statement.executeUpdate();
        } finally {
            databaseService.close();
        }
    }

    @Test
    public void printsHelpForCliRequests() {
        assertEquals(0, CliMain.run(new String[] {"--help"}));
    }

    @Test
    public void createsAccountViaCliCommand() throws Exception {
        int exitCode = CliMain.run(new String[] {
            "account",
            "create",
            "--email",
            email,
            "--phone",
            phone,
            "--password",
            "CorrectHorse!",
            "--contact",
            "email"
        });

        assertEquals(0, exitCode);

        DatabaseService databaseService = DatabaseService.getInstance();
        try {
            assertNotNull(databaseService.findUserEmail(email));
        } finally {
            databaseService.close();
        }
    }
}
