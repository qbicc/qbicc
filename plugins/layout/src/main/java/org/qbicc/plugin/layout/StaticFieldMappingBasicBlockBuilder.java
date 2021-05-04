package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.GlobalVariableElement;

/**
 *
 */
public class StaticFieldMappingBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, ValueHandle> {
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition currentType;

    public StaticFieldMappingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        currentType = getCurrentElement().getEnclosingType();
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        return intToBool(handle, super.load(map(handle), mode));
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return super.store(map(handle), boolToInt(handle, value), mode);
    }

    @Override
    public Value addressOf(ValueHandle handle) {
        return super.addressOf(map(handle));
    }

    @Override
    public Value arrayLength(ValueHandle arrayHandle) {
        return super.arrayLength(map(arrayHandle));
    }

    @Override
    public ValueHandle visit(Void param, ElementOf node) {
        return elementOf(map(node.getValueHandle()), node.getIndex());
    }

    @Override
    public ValueHandle visit(Void param, GlobalVariable node) {
        return node;
    }

    @Override
    public ValueHandle visit(Void param, InstanceFieldOf node) {
        return instanceFieldOf(map(node.getValueHandle()), node.getVariableElement());
    }

    @Override
    public ValueHandle visit(Void param, LocalVariable node) {
        return node;
    }

    @Override
    public ValueHandle visit(Void param, MemberOf node) {
        return memberOf(map(node.getValueHandle()), node.getMember());
    }

    @Override
    public ValueHandle visit(Void param, StaticField node) {
        Layout layout = Layout.get(ctxt);
        CompoundType.Member member = layout.getStaticFieldMember(node.getVariableElement());
        GlobalVariableElement statics = layout.getStatics();
        ctxt.getImplicitSection(currentType).declareData(null, statics.getName(), statics.getType());
        return memberOf(globalVariable(statics), member);
    }

    @Override
    public ValueHandle visit(Void param, PointerHandle node) {
        return node;
    }

    @Override
    public ValueHandle visit(Void param, ReferenceHandle node) {
        return node;
    }

    private Value boolToInt(final ValueHandle handle, final Value value) {
        if (handle.getValueType() instanceof BooleanType) {
            // we have to widen the value to an integer
            return extend(value, ctxt.getTypeSystem().getUnsignedInteger8Type());
        }
        return value;
    }

    private Value intToBool(final ValueHandle handle, final Value value) {
        ValueType valueType = handle.getValueType();
        if (valueType instanceof BooleanType) {
            // narrow it back
            return truncate(value, (BooleanType) valueType);
        }
        return value;
    }

    private ValueHandle map(final ValueHandle handle) {
        return handle.accept(this, null);
    }
}
