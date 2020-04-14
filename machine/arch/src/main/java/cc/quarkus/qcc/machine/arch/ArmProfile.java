package cc.quarkus.qcc.machine.arch;

/**
 *
 */
public enum ArmProfile {
    Application('A'),
    RealTime('R'),
    Microcontroller('M'),
    Classic('C'),
    ;

    private final char letter;

    ArmProfile(final char letter) {
        this.letter = letter;
    }

    public char getLetter() {
        return letter;
    }
}
