package org.qbicc.machine.llvm.debuginfo;

public enum DISPFlags {
    LocalToUnit("DISPFlagLocalToUnit"),
    Definition("DISPFlagDefinition"),
    Optimized("DISPFlagOptimized"),
    Pure("DISPFlagPure"),
    Elemental("DISPFlagElemental"),
    Recursive("DISPFlagRecursive"),
    MainSubprogram("DISPFlagMainSubprogram"),
    Deleted("DISPFlagDeleted"),
    ObjCDirect("DISPFlagObjCDirect");

    public final String name;

    DISPFlags(String name) {
        this.name = name;
    }
}
