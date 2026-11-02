package AIChat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class AgentMemory {
    private static final int MAX_TURNS = 10;
    private final Deque<String> turns = new ArrayDeque<>();

    public synchronized void remember(String question, String answer) {
        turns.addLast("User: " + question + "\nAssistant: " + answer);
        while (turns.size() > MAX_TURNS) {
            turns.removeFirst();
        }
    }

    public synchronized List<String> getRecentTurns() {
        return Collections.unmodifiableList(new ArrayList<>(turns));
    }

    public synchronized String asPromptContext() {
        StringBuilder context = new StringBuilder();
        for (String turn : turns) {
            context.append(turn).append('\n');
        }
        return context.toString();
    }
}
