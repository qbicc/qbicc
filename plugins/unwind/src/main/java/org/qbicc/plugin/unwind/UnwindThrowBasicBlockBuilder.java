package org.qbicc.plugin.unwind;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.type.StructType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.MethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * The basic block builder which implements {@code Throw} using {@code _Unwind_RaiseException}.
 */
public class UnwindThrowBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public UnwindThrowBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        // todo: skip if noThrows
        this.ctxt = getContext();
    }

    public BasicBlock throw_(final Value value) {
        TypeSystem ts = getTypeSystem();
        UnwindExceptionStrategy teh = UnwindExceptionStrategy.get(ctxt);

        Value ptr;
        final LiteralFactory lf = getLiteralFactory();
        if (getRootElement() instanceof FunctionElement fe) {
            ProgramModule programModule = ctxt.getOrAddProgramModule(fe.getEnclosingType());
            StructType threadNativeType = (StructType) ctxt.getBootstrapClassContext().resolveTypeFromClassName("jdk/internal/thread", "ThreadNative$thread_native");
            DataDeclaration decl = programModule.declareData(null, "_qbicc_bound_java_thread", threadNativeType.getPointer());
            decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            ptr = memberOf(load(lf.literalOf(decl)), teh.getUnwindExceptionMember());
        } else {
            ptr = memberOf(getParam(getEntryLabel(), Slot.thread()), teh.getUnwindExceptionMember());
        }

        String functionName = "_Unwind_RaiseException";
        MethodType origType = teh.getRaiseExceptionMethod().getType();
        FunctionType functionType = ts.getFunctionType(origType.getReturnType(), origType.getParameterTypes());
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(teh.getRaiseExceptionMethod(), functionName, functionType);
        return getFirstBuilder().callNoReturn(lf.literalOf(decl), List.of(ptr));
    }
}
