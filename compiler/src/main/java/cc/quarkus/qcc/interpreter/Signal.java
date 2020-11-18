package cc.quarkus.qcc.interpreter;

/**
 * The set of possible signals that can be sent to a hosted VM.  This is a subset of the set of signals
 * typically available to "real" operating systems.
 */
public enum Signal {
    INT(2),
    QUIT(3),
    KILL(9),
    TERM(15),
    ;
    private final int number;

    Signal(final int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
