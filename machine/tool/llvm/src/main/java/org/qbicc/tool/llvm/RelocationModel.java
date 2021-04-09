package org.qbicc.tool.llvm;

public enum RelocationModel {
    Static("static"),
    Pic("pic"),
    DynamicNoPic("dynamic-no-pic"),
    Ropi("ropi"),
    Rwpi("rwpi"),
    RopiRwpi("ropi-rwpi");

    public final String value;

    RelocationModel(String value) {
        this.value = value;
    }
}
