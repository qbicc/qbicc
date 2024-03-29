package org.qbicc.plugin.patcher;

import static org.qbicc.graph.atomic.AccessModes.GlobalPlain;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
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
    public Value load(Value pointer, ReadAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral staticField) {
            FieldElement field = staticField.getVariableElement();
            VmObject accessor = getAccessor(field);
            if (accessor != null) {
                BasicBlockBuilder fb = getFirstBuilder();
                LiteralFactory lf = ctxt.getLiteralFactory();
                TypeDescriptor fieldDesc = field.getTypeDescriptor();
                @SuppressWarnings("SuspiciousMethodCalls")
                String getter = GETTER_NAMES.getOrDefault(fieldDesc, "get");
                MethodDescriptor desc = MethodDescriptor.synthesize(accessor.getVmClass().getTypeDefinition().getContext(), fieldDesc, List.of());
                ObjectLiteral accessorValue = lf.literalOf(accessor);
                return fb.call(fb.lookupVirtualMethod(accessorValue, accessor.getVmClass().getTypeDefinition().getDescriptor(), getter, desc), accessorValue, List.of());
            }
        }
        return super.load(pointer, accessMode);
    }

    @Override
    public Node store(Value pointer, Value value, WriteAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral staticField) {
            FieldElement field = staticField.getVariableElement();
            VmObject accessor = getAccessor(field);
            if (accessor != null) {
                BasicBlockBuilder fb = getFirstBuilder();
                LiteralFactory lf = ctxt.getLiteralFactory();
                TypeDescriptor fieldDesc = field.getTypeDescriptor();
                MethodDescriptor desc = MethodDescriptor.synthesize(accessor.getVmClass().getTypeDefinition().getContext(), BaseTypeDescriptor.V, List.of(fieldDesc));
                ObjectLiteral accessorValue = lf.literalOf(accessor);
                return fb.call(fb.lookupVirtualMethod(accessorValue, accessor.getVmClass().getTypeDefinition().getDescriptor(), "set", desc), accessorValue, List.of(value));
            }
        }
        return super.store(pointer, value, accessMode);
    }

    @Override
    public Value cmpAndSwap(Value target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        checkAtomicAccessor(target);
        return super.cmpAndSwap(target, expect, update, readMode, writeMode, strength);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (op == ReadModifyWrite.Op.SET) {
            if (pointer instanceof StaticFieldLiteral sfl && getAccessor(sfl.getVariableElement()) != null) {
                if (GlobalPlain.includes(readMode) || GlobalPlain.includes(writeMode)) {
                    Value loaded = load(pointer, readMode);
                    store(pointer, update, writeMode);
                    return loaded;
                } else {
                    atomicNotAllowed();
                }
            }
        } else {
            checkAtomicAccessor(pointer);
        }
        return super.readModifyWrite(pointer, op, update, readMode, writeMode);
    }

    private void checkAtomicAccessor(final Value pointer) {
        if (pointer instanceof StaticFieldLiteral sfl && getAccessor(sfl.getVariableElement()) != null) {
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
