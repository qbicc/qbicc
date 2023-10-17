package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.List;

public class AbortingThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public AbortingThrowLoweringBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    public BasicBlock throw_(final Value value) {
        TypeSystem ts = ctxt.getTypeSystem();
        ExecutableElement el = getDelegate().getRootElement();
        ProgramModule programModule = ctxt.getOrAddProgramModule(el);
        FunctionType abortSignature = ts.getFunctionType(ts.getVoidType(), List.of());
        FunctionDeclaration fd = programModule.declareFunction(null, "abort", abortSignature, Function.FN_NO_RETURN, SafePointBehavior.ALLOWED);
        return callNoReturn(getLiteralFactory().literalOf(fd), List.of());
    }
}
