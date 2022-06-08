package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.AddressNaming;
import org.qbicc.machine.llvm.DllStorageClass;
import org.qbicc.machine.llvm.Global;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.RuntimePreemption;
import org.qbicc.machine.llvm.ThreadLocalStorageModel;
import org.qbicc.machine.llvm.Visibility;
import io.smallrye.common.constraint.Assert;

import static org.qbicc.machine.arch.AddressSpaceConstants.DEFAULT;

/**
 *
 */
final class GlobalImpl extends AbstractYieldingInstruction implements Global {
    final boolean constant;
    final AbstractValue type;
    Linkage linkage = Linkage.EXTERNAL;
    Visibility visibility = Visibility.DEFAULT;
    DllStorageClass dllStorageClass = DllStorageClass.NONE;
    AddressNaming addressNaming = AddressNaming.NAMED;
    RuntimePreemption preemption = RuntimePreemption.PREEMPTABLE;
    String section;
    int addressSpace = 0;
    int alignment = 0;
    AbstractValue value;
    ThreadLocalStorageModel threadLocalStorageModel;
    boolean appending;

    GlobalImpl(final ModuleImpl module, final boolean constant, final AbstractValue type) {
        super(module);
        this.constant = constant;
        this.type = type;
    }

    public Global value(final LLValue value) {
        this.value = (AbstractValue) value;
        return this;
    }

    public Global meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Global comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public Global dllStorageClass(final DllStorageClass dllStorageClass) {
        this.dllStorageClass = Assert.checkNotNullParam("dllStorageClass", dllStorageClass);
        return this;
    }

    public Global alignment(final int alignment) {
        this.alignment = alignment;
        return this;
    }

    public Global preemption(final RuntimePreemption preemption) {
        this.preemption = Assert.checkNotNullParam("preemption", preemption);
        return this;
    }

    public Global section(final String section) {
        this.section = Assert.checkNotNullParam("section", section);
        return this;
    }

    public Global linkage(final Linkage linkage) {
        this.linkage = Assert.checkNotNullParam("linkage", linkage);
        return this;
    }

    public Global visibility(final Visibility visibility) {
        this.visibility = Assert.checkNotNullParam("visibility", visibility);
        return this;
    }

    public Global threadLocal(final ThreadLocalStorageModel model) {
        this.threadLocalStorageModel = Assert.checkNotNullParam("model", model);
        return this;
    }

    public Global addressSpace(final int addressSpace) {
        this.addressSpace = addressSpace;
        return this;
    }

    public Global appending() {
        this.appending = true;
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        final Linkage linkage = this.linkage;
        AbstractValue value = this.value;
        if (value == null || linkage != Linkage.EXTERNAL) {
            target.append(linkage.toString());
            target.append(' ');
        }
        final RuntimePreemption preemption = this.preemption;
        if (preemption != RuntimePreemption.PREEMPTABLE) {
            target.append(preemption.toString());
            target.append(' ');
        }
        final Visibility visibility = this.visibility;
        if (visibility != Visibility.DEFAULT) {
            target.append(visibility.toString());
            target.append(' ');
        }
        final DllStorageClass dllStorageClass = this.dllStorageClass;
        if (dllStorageClass != DllStorageClass.NONE) {
            target.append(dllStorageClass.toString());
            target.append(' ');
        }
        ThreadLocalStorageModel model = this.threadLocalStorageModel;
        if (model != null) {
            target.append("thread_local");
            if (model != ThreadLocalStorageModel.GENERAL_DYNAMIC) {
                target.append('(');
                target.append(model.toString());
                target.append(')');
            }
            target.append(' ');
        }
        final AddressNaming addressNaming = this.addressNaming;
        if (addressNaming != AddressNaming.NAMED) {
            target.append(addressNaming.toString());
            target.append(' ');
        }
        final int addressSpace = this.addressSpace;
        if (addressSpace != DEFAULT) {
            target.append("addrspace(");
            target.append(Integer.toString(addressSpace));
            target.append(") ");
        }
        if (appending) {
            target.append("appending");
            target.append(' ');
        }
        if (constant) {
            target.append("constant");
        } else {
            target.append("global");
        }
        target.append(' ');
        type.appendTo(target);
        if (value != null) {
            target.append(' ');
            value.appendTo(target);
        }
        String section = this.section;
        if (section != null) {
            target.append(',');
            target.append(' ');
            target.append("section");
            target.append(' ');
            target.append('"');
            target.append(section);
            target.append('"');
        }
        int alignment = this.alignment;
        if (alignment != 0) {
            target.append(',');
            target.append(' ');
            target.append("align");
            target.append(' ');
            target.append(Integer.toString(alignment));
        }
        appendMeta(target);
        return target;
    }
}
