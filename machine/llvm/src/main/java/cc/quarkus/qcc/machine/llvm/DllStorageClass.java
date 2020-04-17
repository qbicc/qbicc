package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public enum DllStorageClass {
    NONE("none"),
    IMPORT("dll" + "import"),
    EXPORT("dll" + "export"),
    ;

    private final String name;

    DllStorageClass(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
