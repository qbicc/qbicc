package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.llvm.Triple;

final class TripleImpl extends AbstractEmittable implements Triple {
    private String arch = "unknown";
    private String vendor = "unknown";
    private String os = "unknown";
    private String env;

    TripleImpl() {
    }

    @Override
    public Triple arch(String arch) {
        this.arch = Assert.checkNotNullParam("arch", arch);
        return this;
    }

    @Override
    public Triple vendor(String vendor) {
        this.vendor = Assert.checkNotNullParam("vendor", vendor);
        return this;
    }

    @Override
    public Triple os(String os) {
        this.os = Assert.checkNotNullParam("os", os);
        return this;
    }

    @Override
    public Triple env(String env) {
        this.env = env;
        return this;
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("target triple = ");
        StringBuilder b = new StringBuilder(50);
        b.append(arch).append('-').append(vendor).append('-').append(os);
        String env = this.env;
        if (env != null) {
            b.append('-').append(env);
        }
        appendEscapedString(target, b.toString());
        return target;
    }
}
