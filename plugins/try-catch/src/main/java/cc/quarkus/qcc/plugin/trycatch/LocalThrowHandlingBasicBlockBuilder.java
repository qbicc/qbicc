package cc.quarkus.qcc.plugin.trycatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ReferenceType;

/**
 * The basic block builder which handles any "local" {@code throw} to a handler in the same method or function.
 */
public class LocalThrowHandlingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public LocalThrowHandlingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    public BasicBlock throw_(final Value value) {
        ReferenceType valueType = (ReferenceType) value.getType();
        Try.CatchMapper catchMapper = getCatchMapper();
        TypeIdLiteral upperBound = valueType.getUpperBound();
        // check to see if there is any possible local handler
        Value tid = typeIdOf(value);
        int cnt = catchMapper.getCatchCount();
        for (int i = 0; i < cnt; i ++) {
            ClassTypeIdLiteral catchType = catchMapper.getCatchType(i);
            if (catchType.isSubtypeOf(upperBound)) {
                // it's a possible match
                BlockLabel resumeLabel = new BlockLabel();
                // compare the thrown type; if it is >= the catch type, go to the exception handler block
                BlockLabel handlerLabel = catchMapper.getCatchHandler(i);
                // may recursively process catch handler block
                catchMapper.setCatchValue(i, if_(cmpGe(tid, upperBound), handlerLabel, resumeLabel), value);
                begin(resumeLabel);
            }
        }
        // otherwise propagate to caller
        return super.throw_(value);
    }
}
