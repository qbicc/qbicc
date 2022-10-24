package org.qbicc.plugin.gc.common.safepoint;

import static org.qbicc.type.descriptor.MethodDescriptor.VOID_METHOD_DESCRIPTOR;

import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;

/**
 * The base class for safe point polling strategy implementation.
 */
public abstract class AbstractSafePointStrategy {
    static final String SAFE_POINT_INT_NAME = "org/qbicc/runtime/main/SafePoint";

    private static final String ENTER_SAFE_POINT = "enterSafePoint";
    private static final String REQUEST_GLOBAL_SAFE_POINT = "requestGlobalSafePoint";
    private static final String CLEAR_GLOBAL_SAFE_POINT = "clearGlobalSafePoint";

    protected final CompilationContext ctxt;

    /**
     * Construct a new instance.
     * The constructor should inject any additional fields or methods needed to implement the strategy.
     *
     * @param ctxt the compilation context
     */
    protected AbstractSafePointStrategy(CompilationContext ctxt) {
        this.ctxt = ctxt;
        final Patcher patcher = Patcher.get(ctxt);
        patcher.replaceMethodBody(ctxt.getBootstrapClassContext(), SAFE_POINT_INT_NAME, REQUEST_GLOBAL_SAFE_POINT, VOID_METHOD_DESCRIPTOR, adapt(this::implementRequestGlobalSafePoint), 0);
        patcher.replaceMethodBody(ctxt.getBootstrapClassContext(), SAFE_POINT_INT_NAME, CLEAR_GLOBAL_SAFE_POINT, VOID_METHOD_DESCRIPTOR, adapt(this::implementClearGlobalSafePoint), 0);
    }

    static MethodBodyFactory adapt(Consumer<BasicBlockBuilder> consumer) {
        return (index, element) -> {
            BasicBlockBuilder bbb = element.getEnclosingType().getContext().newBasicBlockBuilder(element);
            final List<ParameterValue> params = List.of();
            bbb.startMethod(params);
            consumer.accept(bbb);
            bbb.finish();
            final BasicBlock entry = bbb.getFirstBlock();
            return MethodBody.of(entry, Schedule.forMethod(entry), null, params);
        };
    }

    public final void registerReachableMethods(CompilationContext ctxt) {
        final LoadedTypeDefinition safePointClass = ctxt.getBootstrapClassContext().findDefinedType(SAFE_POINT_INT_NAME).load();
        forEachSafePointMethod(name -> ctxt.enqueue(safePointClass.requireSingleMethod(name)));
    }

    protected void forEachSafePointMethod(Consumer<String> consumer) {
        consumer.accept(ENTER_SAFE_POINT);
        consumer.accept(REQUEST_GLOBAL_SAFE_POINT);
        consumer.accept(CLEAR_GLOBAL_SAFE_POINT);
    }

    /**
     * Implement the {@code SafePoint} node.
     * This method is called during lowering to implement the actual action of a safepoint poll.
     *
     * @param bbb the block builder (not {@code null})
     */
    public abstract void safePoint(BasicBlockBuilder bbb);

    /**
     * Implement the method which requests a global safepoint.
     * The implementation must not throw any exception, poll for a safepoint, or call any method that may do either.
     *
     * @param bbb the block builder (not {@code null})
     */
    public abstract void implementRequestGlobalSafePoint(BasicBlockBuilder bbb);

    /**
     * Implement the method which clears a global safepoint request.
     * The implementation must not throw any exception, poll for a safepoint, or call any method that may do either.
     *
     * @param bbb the block builder (not {@code null})
     */
    public abstract void implementClearGlobalSafePoint(BasicBlockBuilder bbb);
}
