package org.qbicc.plugin.lowering;

import java.util.LinkedHashSet;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 *
 */
public final class LocalVariableFindingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final LinkedHashSet<LocalVariableElement> usedVariables;

    public LocalVariableFindingBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        usedVariables = Lowering.get(ctxt).createUsedVariableSet(getCurrentElement());
    }

    @Override
    public ValueHandle localVariable(LocalVariableElement variable) {
        return super.localVariable(record(variable));
    }

    private LocalVariableElement record(final LocalVariableElement variable) {
        usedVariables.add(variable);
        return variable;
    }
}
