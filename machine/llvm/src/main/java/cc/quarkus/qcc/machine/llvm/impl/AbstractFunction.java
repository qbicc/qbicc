package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.AddressNaming;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.DllStorageClass;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Metable;
import cc.quarkus.qcc.machine.llvm.Visibility;
import cc.quarkus.qcc.machine.llvm.Function;
import cc.quarkus.qcc.machine.llvm.Value;
import io.smallrye.common.constraint.Assert;

abstract class AbstractFunction extends AbstractMetable implements Function {
    final String name;
    Linkage linkage = Linkage.EXTERNAL;
    Visibility visibility = Visibility.DEFAULT;
    DllStorageClass dllStorageClass = DllStorageClass.NONE;
    CallingConvention callingConvention = CallingConvention.C;
    AddressNaming addressNaming = AddressNaming.NAMED;
    AbstractValue returnType;
    int addressSpace = 0;
    // todo: return type attribute
    int alignment = 0;
    // todo: prefix data https://llvm.org/docs/LangRef.html#prefixdata
    // todo: prologue data https://llvm.org/docs/LangRef.html#prologuedata
    ParameterImpl lastParam;

    AbstractFunction(final String name) {
        this.name = name;
    }

    public Function returns(final Value returnType) {
        Assert.checkNotNullParam("returnType", returnType);
        // todo with attributes...
        this.returnType = (AbstractValue) returnType;
        return this;
    }

    public ParameterImpl param(final Value type) {
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

    public Function meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    public Function comment(final String comment) {
        super.comment(comment);
        return this;
    }

    ///////////////////
    // Private API
    ///////////////////

    abstract String keyWord();

    ///////////////////
    // Emitting
    ///////////////////

    public final Appendable appendTo(final Appendable target) throws IOException {
        target.append(keyWord());
        target.append(' ');
        final Linkage linkage = this.linkage;
        if (linkage != Linkage.EXTERNAL) {
            target.append(linkage.toString());
            target.append(' ');
        }
        appendAfterLinkage(target);
        return target;
    }

    void appendAfterLinkage(final Appendable target) throws IOException {
        final Visibility visibility = this.visibility;
        if (visibility != Visibility.DEFAULT) {
            target.append(visibility.toString());
            target.append(' ');
        }
        appendAfterVisibility(target);
    }

    void appendAfterVisibility(final Appendable target) throws IOException {
        final DllStorageClass dllStorageClass = this.dllStorageClass;
        if (dllStorageClass != DllStorageClass.NONE) {
            target.append(dllStorageClass.toString());
            target.append(' ');
        }
        appendAfterDllStorageClass(target);
    }

    void appendAfterDllStorageClass(final Appendable target) throws IOException {
        final CallingConvention callingConvention = this.callingConvention;
        if (callingConvention != CallingConvention.C) {
            target.append(callingConvention.toString());
            target.append(' ');
        }
        appendAfterCallingConvention(target);
    }

    void appendAfterCallingConvention(final Appendable target) throws IOException {
        final AddressNaming addressNaming = this.addressNaming;
        if (addressNaming != AddressNaming.NAMED) {
            target.append(addressNaming.toString());
            target.append(' ');
        }
        appendAfterAddressNaming(target);
    }

    void appendAfterAddressNaming(final Appendable target) throws IOException {
        final int addressSpace = this.addressSpace;
        if (addressSpace != 0) {
            target.append("addrspace(");
            target.append(Integer.toString(addressSpace));
            target.append(") ");
        }
        appendAfterAddressSpace(target);
    }

    void appendAfterAddressSpace(final Appendable target) throws IOException {
        returnType.appendTo(target);
        target.append(' ');
        appendAfterReturnType(target);
    }

    void appendAfterReturnType(final Appendable target) throws IOException {
        // todo: param attr
        appendAfterReturnTypeAttribute(target);
    }

    void appendAfterReturnTypeAttribute(final Appendable target) throws IOException {
        target.append('@');
        target.append(name);
        appendAfterName(target);
    }

    void appendAfterName(final Appendable target) throws IOException {
        target.append('(');
        final ParameterImpl lastParam = this.lastParam;
        if (lastParam != null) {
            lastParam.appendTo(target);
        }
        target.append(')');
        appendAfterParams(target);
    }

    void appendAfterParams(final Appendable target) throws IOException {
        if (alignment != 0) {
            target.append("align ");
            target.append(Integer.toString(alignment));
        }
        appendAfterAlignment(target);
    }

    void appendAfterAlignment(final Appendable target) throws IOException {
        // todo: GC name?
        appendAfterGc(target);
    }

    void appendAfterGc(final Appendable target) throws IOException {
        // todo: prefix
        appendAfterPrefix(target);
    }

    void appendAfterPrefix(final Appendable target) throws IOException {
        // todo: prologue
        appendAfterPrologue(target);
    }

    void appendAfterPrologue(final Appendable target) throws IOException {
        // nothing
    }


    static final class ParameterImpl extends AbstractEmittable implements Parameter {
        String name;
        final ParameterImpl prev;
        final AbstractFunction function;
        final AbstractValue type;

        ParameterImpl(final ParameterImpl prev, final AbstractFunction function, final AbstractValue type) {
            this.prev = prev;
            this.function = function;
            this.type = type;
        }

        public ParameterImpl param(final Value type) {
            return function.param(type);
        }

        public ParameterImpl name(final String name) {
            this.name = name;
            return this;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(',').append(' ');
            }
            type.appendTo(target);
            if (name != null) {
                target.append(' ').append(name);
            }
            return target;
        }
    }
}
