package app;

import AccountCreation.LoginGUI;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && !isGuiCommand(args[0])) {
            CliMain.main(args);
            return;
        }
        launchGui(args);
    }

    public static void launchGui(String[] args) {
        LoginGUI.main(args);
    }

    private static boolean isGuiCommand(String command) {
        return "gui".equalsIgnoreCase(command)
                || "--gui".equalsIgnoreCase(command);
    }
}
