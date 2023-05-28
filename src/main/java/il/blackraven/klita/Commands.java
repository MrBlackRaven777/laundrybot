package il.blackraven.klita;

public enum Commands {
    START("/start"),
    USE("/use");



    private final String command;
    private Commands(String command) {
        this.command = command;
    }

    public String get() {
        return this.command;
    }
}
