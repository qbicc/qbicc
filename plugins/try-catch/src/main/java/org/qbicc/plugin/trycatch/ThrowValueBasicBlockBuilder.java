package org.qbicc.plugin.trycatch;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public class ThrowValueBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowValueBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        FieldElement exceptionField = ctxt.getExceptionField();
        store(instanceFieldOf(referenceHandle(currentThread()), exceptionField), value, MemoryAtomicityMode.NONE);
        // the actual throw is lowered by a back end operation, and doesn't depend on the value itself
        return super.throw_(value);
    }
}
