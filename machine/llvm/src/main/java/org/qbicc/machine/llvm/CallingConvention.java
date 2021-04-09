package org.qbicc.machine.llvm;

/**
 *
 */
public enum CallingConvention {
    C("ccc"),
    FAST("fast" + "cc"),
    COLD("cold" + "cc"),
    CC_10("cc 10"),
    CC_11("cc 11"),
    WEBKIT_JS("webkit_js" + "cc"),
    ANYREG("anyreg" + "cc"),
    PRESERVE_MOST("preserve_most" + "cc"),
    PRESERVE_ALL("preserve_all" + "cc"),
    CXX_FAST_TLS("cxx_fast_tls"+ "cc"),
    SWIFT("swift" + "cc"),
    TAIL("tail" + "cc"),
    CFGUARD_CHECK("cfguard_check" + "cc"),
    // todo CC_64 and up
    ;

    private final String name;

    CallingConvention(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
