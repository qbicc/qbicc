package org.qbicc.plugin.patcher;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class AccessorBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Map<BaseTypeDescriptor, String> GETTER_NAMES = Map.of(
        BaseTypeDescriptor.B, "getAsByte",
        BaseTypeDescriptor.C, "getAsChar",
        BaseTypeDescriptor.D, "getAsDouble",
        BaseTypeDescriptor.F, "getAsFloat",
        BaseTypeDescriptor.I, "getAsInt",
        BaseTypeDescriptor.J, "getAsLong",
        BaseTypeDescriptor.S, "getAsShort",
        BaseTypeDescriptor.Z, "getAsBoolean"
    );

    private final CompilationContext ctxt;

    public AccessorBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value load(PointerValue handle, ReadAccessMode accessMode) {
        if (handle instanceof StaticField staticField) {
            FieldElement field = staticField.getVariableElement();
            VmObject accessor = getAccessor(field);
            if (accessor != null) {
                BasicBlockBuilder fb = getFirstBuilder();
                LiteralFactory lf = ctxt.getLiteralFactory();
                TypeDescriptor fieldDesc = field.getTypeDescriptor();
                @SuppressWarnings("SuspiciousMethodCalls")
                String getter = GETTER_NAMES.getOrDefault(fieldDesc, "get");
                MethodDescriptor desc = MethodDescriptor.synthesize(accessor.getVmClass().getTypeDefinition().getContext(), fieldDesc, List.of());
                return fb.call(fb.virtualMethodOf(lf.literalOf(accessor), accessor.getVmClass().getTypeDefinition().getDescriptor(), getter, desc), List.of());
            }
        }
        return super.load(handle, accessMode);
    }

    @Override
    public Node store(PointerValue handle, Value value, WriteAccessMode accessMode) {
        if (handle instanceof StaticField staticField) {
            FieldElement field = staticField.getVariableElement();
            VmObject accessor = getAccessor(field);
            if (accessor != null) {
                BasicBlockBuilder fb = getFirstBuilder();
                LiteralFactory lf = ctxt.getLiteralFactory();
                TypeDescriptor fieldDesc = field.getTypeDescriptor();
                MethodDescriptor desc = MethodDescriptor.synthesize(accessor.getVmClass().getTypeDefinition().getContext(), BaseTypeDescriptor.V, List.of(fieldDesc));
                return fb.call(fb.virtualMethodOf(lf.literalOf(accessor), accessor.getVmClass().getTypeDefinition().getDescriptor(), "set", desc), List.of(value));
            }
        }
        return super.store(handle, value, accessMode);
    }

    @Override
    public Value cmpAndSwap(PointerValue target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        checkAtomicAccessor(target);
        return super.cmpAndSwap(target, expect, update, readMode, writeMode, strength);
    }

    @Override
    public Value readModifyWrite(PointerValue target, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (op == ReadModifyWrite.Op.SET) {
            if (target instanceof StaticField sf && getAccessor(sf.getVariableElement()) != null) {
                if (GlobalPlain.includes(readMode) || GlobalPlain.includes(writeMode)) {
                    Value loaded = load(target, readMode);
                    store(target, update, writeMode);
                    return loaded;
                } else {
                    atomicNotAllowed();
                }
            }
        } else {
            checkAtomicAccessor(target);
        }
        return super.readModifyWrite(target, op, update, readMode, writeMode);
    }

    private void checkAtomicAccessor(final PointerValue target) {
        if (target instanceof StaticField sf && getAccessor(sf.getVariableElement()) != null) {
            atomicNotAllowed();
        }
    }

    private void atomicNotAllowed() {
        ctxt.error(getLocation(), "Atomic operation not supported for fields with accessors");
    }

    private VmObject getAccessor(FieldElement fieldElement) {
        return Patcher.get(ctxt).lookUpAccessor(fieldElement);
    }
}
