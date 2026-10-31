package DashboardCategories;

import java.io.IOException;

public interface ChatGPT5API {
    ChatGPT5Response chatCompletion(String prompt) throws IOException;
}
