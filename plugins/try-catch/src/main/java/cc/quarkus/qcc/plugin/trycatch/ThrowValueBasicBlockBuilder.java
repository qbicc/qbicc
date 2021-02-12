package cc.quarkus.qcc.plugin.trycatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.element.FieldElement;

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
