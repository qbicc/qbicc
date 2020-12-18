package cc.quarkus.qcc.plugin.trycatch;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.NullType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 * The basic block builder which handles any "local" {@code throw} to a handler in the same method or function.
 */
public class LocalThrowHandlingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public LocalThrowHandlingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    public BasicBlock throw_(final Value value) {
        if (value.getType() instanceof NullType) {
            // todo: this should move to a general null-checking plugin as an `if`
            ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
            ValidatedTypeDefinition npe = classContext.findDefinedType("java/lang/NullPointerException").validate();
            Value ex = new_(npe.getClassType());
            ex = invokeConstructor(ex, npe.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
            return throw_(ex);
        }
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler == null) {
            // propagate to caller
            return super.throw_(value);
        }
        // instead, dispatch to the exception handler
        BasicBlock from = goto_(exceptionHandler.getHandler());
        exceptionHandler.enterHandler(from, value);
        return from;
    }
}
