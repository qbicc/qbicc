package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public enum ThreadLocalStorageModel {
    GENERAL_DYNAMIC("general" + "dynamic"),
    LOCAL_DYNAMIC("local" + "dynamic"),
    INITIAL_EXEC("initial" + "exec"),
    LOCAL_EXEC("local" + "exec"),
    ;
    private final String name;

    ThreadLocalStorageModel(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
