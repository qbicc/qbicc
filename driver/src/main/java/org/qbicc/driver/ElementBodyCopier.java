package org.qbicc.driver;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An element handler which copies the body of the method.
 */
public final class ElementBodyCopier implements Consumer<ExecutableElement> {
    /**
     * Construct a new instance.
     */
    public ElementBodyCopier() {
    }

    @Override
    public void accept(ExecutableElement element) {
        // rewrite the method body
        if (element.hasMethodBody()) {
            ClassContext classContext = element.getEnclosingType().getContext();
            CompilationContext compilationContext = classContext.getCompilationContext();
            BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock>, NodeVisitor<Node.Copier, Value, Node, BasicBlock>> copier = compilationContext.getCopier();
            MethodBody original = element.getMethodBody();
            BasicBlock entryBlock = original.getEntryBlock();
            BasicBlockBuilder builder = classContext.newBasicBlockBuilder(element);
            BasicBlock copyBlock = Node.Copier.execute(entryBlock, builder, compilationContext, copier);
            builder.finish();
            element.replaceMethodBody(MethodBody.of(copyBlock, original.getParameterSlots()));
        }
    }
}
