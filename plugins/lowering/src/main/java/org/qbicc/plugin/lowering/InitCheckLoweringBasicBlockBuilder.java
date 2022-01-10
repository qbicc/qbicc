package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;

public class InitCheckLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public InitCheckLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Node initCheck(InitializerElement initializer, Value initThunk) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        MethodElement run = RuntimeMethodFinder.get(ctxt).getMethod("org/qbicc/runtime/main/Once", "run");

        final BlockLabel callInit = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        Value done = load(getFirstBuilder().instanceFieldOf(referenceHandle(initThunk), run.getEnclosingType().load().findField("done")));
        if_(isNe(done, lf.literalOf(ctxt.getTypeSystem().getSignedInteger8Type(), 0)), goAhead, callInit);
        try {
            begin(callInit);
            getFirstBuilder().call(getFirstBuilder().virtualMethodOf(initThunk, run, run.getDescriptor(), run.getType()), List.of());
            goto_(goAhead);
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(goAhead);

        return nop();
    }
}
