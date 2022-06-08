package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.qbicc.machine.llvm.AddressNaming;
import org.qbicc.machine.llvm.CallingConvention;
import org.qbicc.machine.llvm.DllStorageClass;
import org.qbicc.machine.llvm.Function;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Visibility;
import io.smallrye.common.constraint.Assert;

import static org.qbicc.machine.arch.AddressSpaceConstants.DEFAULT;

abstract class AbstractFunction extends AbstractMetable implements Function {
    final String name;
    final List<AbstractValue> attributes = new ArrayList<>();
    Linkage linkage = Linkage.EXTERNAL;
    Visibility visibility = Visibility.DEFAULT;
    DllStorageClass dllStorageClass = DllStorageClass.NONE;
    CallingConvention callingConvention = CallingConvention.C;
    AddressNaming addressNaming = AddressNaming.NAMED;
    ReturnsImpl returnType;

    int addressSpace = 0;
    // todo: return type attribute
    int alignment = 0;
    boolean variadic;
    // todo: prefix data https://llvm.org/docs/LangRef.html#prefixdata
    // todo: prologue data https://llvm.org/docs/LangRef.html#prologuedata
    ParameterImpl lastParam;

    AbstractFunction(final String name) {
        this.name = name;
    }

    public Returns returns(final LLValue returnType) {
        Assert.checkNotNullParam("returnType", returnType);
        // todo with attributes...
        this.returnType = new ReturnsImpl((AbstractValue) returnType);
        return this.returnType;
    }

    public ParameterImpl param(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return lastParam = new ParameterImpl(lastParam, this, (AbstractValue) type);
    }

    public Function linkage(final Linkage linkage) {
        Assert.checkNotNullParam("linkage", linkage);
        this.linkage = linkage;
        return this;
    }

    public Function visibility(final Visibility visibility) {
        Assert.checkNotNullParam("visibility", visibility);
        this.visibility = visibility;
        return this;
    }

    public Function dllStorageClass(final DllStorageClass dllStorageClass) {
        Assert.checkNotNullParam("dllStorageClass", dllStorageClass);
        this.dllStorageClass = dllStorageClass;
        return this;
    }

    public Function callingConvention(final CallingConvention callingConvention) {
        Assert.checkNotNullParam("callingConvention", callingConvention);
        this.callingConvention = callingConvention;
        return this;
    }

    public Function addressNaming(final AddressNaming addressNaming) {
        Assert.checkNotNullParam("addressNaming", addressNaming);
        this.addressNaming = addressNaming;
        return this;
    }

    public Function addressSpace(final int addressSpace) {
        Assert.checkMinimumParameter("addressSpace", 0, addressSpace);
        this.addressSpace = addressSpace;
        return this;
    }

    public Function alignment(final int alignment) {
        Assert.checkMinimumParameter("alignment", 1, alignment);
        Assert.checkMaximumParameter("alignment", 1 << 29, alignment);
        if (Integer.bitCount(alignment) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of two");
        }
        return this;
    }

    public Function variadic() {
        variadic = true;
        return this;
    }

    public Function attribute(LLValue attribute) {
        attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
        return this;
    }

    public Function meta(final String name, final LLValue data) {
        super.meta(name, data);
        return this;
    }

    public Function comment(final String comment) {
        super.comment(comment);
        return this;
    }

    public LLValue asGlobal() {
        return new NamedGlobalValueOf(this.name);
    }

    protected final void appendLinkage(final Appendable target) throws IOException {
        if (linkage != Linkage.EXTERNAL) {
            target.append(linkage.toString()).append(' ');
        }
    }

    protected final void appendVisibility(final Appendable target) throws IOException {
        if (visibility != Visibility.DEFAULT) {
            target.append(visibility.toString()).append(' ');
        }
    }

    protected final void appendDllStorageClass(final Appendable target) throws IOException {
        if (dllStorageClass != DllStorageClass.NONE) {
            target.append(dllStorageClass.toString()).append(' ');
        }
    }

    protected final void appendCallingConvention(final Appendable target) throws IOException {
        if (callingConvention != CallingConvention.C) {
            target.append(callingConvention.toString()).append(' ');
        }
    }

    protected final void appendNameAndType(final Appendable target) throws IOException {
        returnType.appendTo(target);

        target.append(" @").append(LLVM.needsQuotes(name) ? LLVM.quoteString(name) : name).append('(');

        if (lastParam != null) {
            lastParam.appendTo(target);
            if (variadic) {
                target.append(", ...");
            }
        } else {
            if (variadic) {
                target.append("...");
            }
        }

        target.append(")");
    }

    protected final void appendAddressNaming(final Appendable target) throws IOException {
        if (addressNaming != AddressNaming.NAMED) {
            target.append(' ').append(addressNaming.toString());
        }
    }

    protected final void appendAddressSpace(final Appendable target) throws IOException {
        if (addressSpace != DEFAULT) {
            target.append(" addrspace(").append(Integer.toString(addressSpace)).append(')');
        }
    }

    protected final void appendFunctionAttributes(final Appendable target) throws IOException {
        for (AbstractValue attribute : attributes) {
            target.append(' ');
            attribute.appendTo(target);
        }
    }

    protected final void appendAlign(final Appendable target) throws IOException {
        if (alignment != 0) {
            target.append(" align ").append(Integer.toString(alignment));
        }
    }

    static final class ReturnsImpl extends AbstractEmittable implements Returns {
        final AbstractValue type;
        final List<AbstractValue> attributes = new ArrayList<>();

        ReturnsImpl(final AbstractValue type) {
            this.type = type;
        }

        public ReturnsImpl attribute(LLValue attribute) {
            this.attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
            return this;
        }

        public LLValue type() {
            return type;
        }

        public Appendable appendTo(Appendable target) throws IOException {
            for (AbstractValue attribute : attributes) {
                attribute.appendTo(target);
                target.append(' ');
            }

            type.appendTo(target);

            return target;
        }
    }

    static final class ParameterImpl extends AbstractEmittable implements Parameter {
        String name;
        final ParameterImpl prev;
        final AbstractFunction function;
        final AbstractValue type;
        final List<AbstractValue> attributes = new ArrayList<>();

        ParameterImpl(final ParameterImpl prev, final AbstractFunction function, final AbstractValue type) {
            this.prev = prev;
            this.function = function;
            this.type = type;
        }

        public ParameterImpl param(final LLValue type) {
            return function.param(type);
        }

        public ParameterImpl name(final String name) {
            this.name = name;
            return this;
        }

        public ParameterImpl attribute(final LLValue attribute) {
            attributes.add((AbstractValue) Assert.checkNotNullParam("attribute", attribute));
            return this;
        }

        public LLValue type() {
            return type;
        }

        public LLValue asValue() {
            return new AbstractValue() {
                public Appendable appendTo(final Appendable target) throws IOException {
                    return target.append('%').append(name);
                }
            };
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(',').append(' ');
            }
            type.appendTo(target);
            for (AbstractValue attribute : attributes) {
                target.append(' ');
                attribute.appendTo(target);
            }
            if (name != null) {
                target.append(' ').append('%').append(name);
            }
            return target;
        }
    }
}
