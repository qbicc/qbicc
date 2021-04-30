package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public class StaticFieldRegisteringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public StaticFieldRegisteringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public ValueHandle staticField(FieldElement fieldElement) {
        Layout.get(ctxt).getStaticFieldMember(fieldElement);
        return super.staticField(fieldElement);
    }
}
