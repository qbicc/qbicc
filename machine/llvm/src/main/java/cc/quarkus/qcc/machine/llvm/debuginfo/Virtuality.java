package cc.quarkus.qcc.machine.llvm.debuginfo;

public enum Virtuality {
    None("DW_VIRTUALITY_none"),
    Virtual("DW_VIRTUALITY_virtual"),
    PureVirtual("DW_VIRTUALITY_pure_virtual");

    public final String name;

    Virtuality(final String name) {
        this.name = name;
    }
}
