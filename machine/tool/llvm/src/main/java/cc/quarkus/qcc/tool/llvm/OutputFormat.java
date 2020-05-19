package cc.quarkus.qcc.tool.llvm;

/**
 *
 */
public enum OutputFormat {
    ASM("asm"),
    OBJ("obj"),
    ;

    private final String optionString;

    OutputFormat(final String optionString) {
        this.optionString = optionString;
    }

    public String toOptionString() {
        return optionString;
    }
}
