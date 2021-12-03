package org.qbicc.plugin.opt;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.definition.element.FieldElement;

public class InitializedStaticFieldBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    final CompilationContext ctxt;

    public InitializedStaticFieldBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, ReadAccessMode accessMode) {
        if (handle instanceof StaticField) {
            final FieldElement fieldElement = ((StaticField) handle).getVariableElement();
            if (fieldElement.isReallyFinal()) {
                Value contents = fieldElement.getEnclosingType().load().getInitialValue(fieldElement);
                if (contents instanceof Literal) {
                    // ctxt.info("Replacing "+fieldElement+" with "+contents+" in "+getDelegate().getCurrentElement());
                    return contents;
                }
            }
        }
        return getDelegate().load(handle, accessMode);
    }
}
