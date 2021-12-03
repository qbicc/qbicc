package org.qbicc.plugin.patcher;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
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

    public AccessorBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, ReadAccessMode accessMode) {
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
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
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
        return super.store(handle, value, mode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode, CmpAndSwap.Strength strength) {
        checkAtomicAccessor(target);
        return super.cmpAndSwap(target, expect, update, successMode, failureMode, strength);
    }

    @Override
    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndAdd(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndBitwiseAnd(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndBitwiseNand(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndBitwiseOr(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndBitwiseXor(target, update, atomicityMode);
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (target instanceof StaticField sf && getAccessor(sf.getVariableElement()) != null) {
            if (atomicityMode == MemoryAtomicityMode.NONE || atomicityMode == MemoryAtomicityMode.UNORDERED) {
                Value loaded = load(target, atomicityMode);
                store(target, update, atomicityMode);
                return loaded;
            } else {
                atomicNotAllowed();
            }
        }
        return super.getAndSet(target, update, atomicityMode);
    }

    @Override
    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndSetMax(target, update, atomicityMode);
    }

    @Override
    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndSetMin(target, update, atomicityMode);
    }

    @Override
    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        checkAtomicAccessor(target);
        return super.getAndSub(target, update, atomicityMode);
    }

    private void checkAtomicAccessor(final ValueHandle target) {
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
