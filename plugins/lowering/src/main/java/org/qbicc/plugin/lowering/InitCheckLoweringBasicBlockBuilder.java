package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.dispatch.DispatchTables;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;
import java.util.Map;

import static org.qbicc.graph.atomic.AccessModes.GlobalAcquire;

public class InitCheckLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public InitCheckLoweringBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Node initCheck(InitializerElement initializer, Value initThunk) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        MethodElement run = RuntimeMethodFinder.get(ctxt).getMethod("org/qbicc/runtime/main/Once", "run");
        DispatchTables.get(ctxt).registerRuntimeInitializer(initializer);
        ctxt.enqueue(initializer);

        final BlockLabel callInit = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        Value done = load(getFirstBuilder().instanceFieldOf(decodeReference(initThunk), run.getEnclosingType().load().findField("done")), GlobalAcquire);
        if_(isNe(done, lf.literalOf(ctxt.getTypeSystem().getSignedInteger8Type(), 0)), goAhead, callInit, Map.of());
        try {
            begin(callInit);
            getFirstBuilder().call(getFirstBuilder().virtualMethodOf(initThunk, run), List.of());
            goto_(goAhead, Map.of());
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(goAhead);

        return nop();
    }
}
