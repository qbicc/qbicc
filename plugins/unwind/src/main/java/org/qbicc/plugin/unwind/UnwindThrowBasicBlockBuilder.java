package org.qbicc.plugin.unwind;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.FunctionType;
import org.qbicc.type.MethodType;
import org.qbicc.type.TypeSystem;

/**
 * The basic block builder which implements {@code Throw} using {@code _Unwind_RaiseException}.
 */
public class UnwindThrowBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public UnwindThrowBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    public BasicBlock throw_(final Value value) {
        TypeSystem ts = getTypeSystem();
        UnwindExceptionStrategy teh = UnwindExceptionStrategy.get(ctxt);

        // TODO Is this safe? Can the java/lang/Thread object be moved while this pointer is still in use?
        Value ptr = instanceFieldOf(decodeReference(load(currentThread(), SinglePlain)), teh.getUnwindExceptionField());

        String functionName = "_Unwind_RaiseException";
        MethodType origType = teh.getRaiseExceptionMethod().getType();
        FunctionType functionType = ts.getFunctionType(origType.getReturnType(), origType.getParameterTypes());
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(teh.getRaiseExceptionMethod(), functionName, functionType);
        return getFirstBuilder().callNoReturn(getLiteralFactory().literalOf(decl), List.of(ptr));
    }
}
