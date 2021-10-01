package org.qbicc.plugin.constants;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;

/**
 * A basic block builder which substitutes reads from constant static fields with the constant value of the field.
 */
public class ConstantBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ConstantBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        if (handle instanceof StaticField) {
            final FieldElement fieldElement = ((StaticField) handle).getVariableElement();
            Value constantValue = Constants.get(ctxt).getConstantValue(fieldElement);
            if (constantValue != null) {
                return constantValue;
            }
            if (fieldElement.isReallyFinal()) {
                final Literal initialValue = fieldElement.getInitialValue();
                if (initialValue != null) {
                    return initialValue;
                }
            }
        }
        return getDelegate().load(handle, mode);
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        if (getRootElement() instanceof InitializerElement) {
            if (handle instanceof StaticField) {
                final FieldElement fieldElement = ((StaticField) handle).getVariableElement();
                if (fieldElement.isReallyFinal()) {
                    if (value instanceof Literal && ! (value instanceof ConstantLiteral) || value instanceof OffsetOfField) {
                        Constants.get(ctxt).registerConstant(fieldElement, value);
                    }
                }
            }
        }
        return super.store(handle, value, mode);
    }
}
