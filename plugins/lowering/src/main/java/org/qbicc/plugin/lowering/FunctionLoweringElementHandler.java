package org.qbicc.plugin.lowering;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.object.Function;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * An element handler which lowers each element to a function program object.
 */
public final class FunctionLoweringElementHandler implements Consumer<ExecutableElement> {
    /**
     * Construct a new instance.
     */
    public FunctionLoweringElementHandler() {
    }

    @Override
    public void accept(ExecutableElement element) {
        if (element.hasMethodBody()) {
            // copy to a function.
            ClassContext classContext = element.getEnclosingType().getContext();
            CompilationContext compilationContext = classContext.getCompilationContext();
            BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, PointerValue>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, PointerValue>> copier = compilationContext.getCopier();
            MethodBody original = element.getMethodBody();
            BasicBlock entryBlock = original.getEntryBlock();
            List<Slot> paramSlots;
            paramSlots = original.getParameterSlots();
            if (element instanceof FunctionElement) {
                // it is already a function with a C calling convention and ABI.
            } else {
                // make a function with a Java calling convention and ABI.
                if (! element.isStatic()) {
                    paramSlots = Slot.argListWithPrependedThis(original.getParameterSlots());
                }
                paramSlots = Slot.argListWithPrependedThread(paramSlots);
            }
            Function function = compilationContext.getExactFunction(element);
            // perform the copy.
            BasicBlockBuilder builder = classContext.newBasicBlockBuilder(element);
            BasicBlock copyBlock = Node.Copier.execute(entryBlock, builder, compilationContext, copier);
            builder.finish();
            function.replaceBody(MethodBody.of(copyBlock, Schedule.forMethod(copyBlock), paramSlots));
            element.replaceMethodBody(function.getBody());
        }
    }
}
