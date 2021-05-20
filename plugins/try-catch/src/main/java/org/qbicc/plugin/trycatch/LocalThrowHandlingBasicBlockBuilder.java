package org.qbicc.plugin.trycatch;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * The basic block builder which handles any "local" {@code throw} to a handler in the same method or function.
 */
public class LocalThrowHandlingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public LocalThrowHandlingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler == null) {
            // propagate to caller
            return super.throw_(value);
        }
        LiteralFactory lf = ctxt.getLiteralFactory();
        // null check
        BlockLabel npe = new BlockLabel();
        BasicBlock from = if_(isEq(value, lf.zeroInitializerLiteralOfType(value.getType())), npe, exceptionHandler.getHandler());
        // dispatch to the exception handler
        exceptionHandler.enterHandler(from, value);
        // throw an NPE to the handler instead
        begin(npe);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LoadedTypeDefinition npeType = classContext.findDefinedType("java/lang/NullPointerException").load();
        Value ex = new_(npeType.getClassType());
        // pre-resolver
        ValueHandle ctor = constructorOf(ex, npeType.getDescriptor(), MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of()));
        call(ctor, List.of());
        BasicBlock from2 = goto_(exceptionHandler.getHandler());
        exceptionHandler.enterHandler(from2, ex);
        return from;
    }
}
