package org.qbicc.main;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Condition;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
final class ElementReachableAdapter implements Consumer<CompilationContext> {
    private final Consumer<ExecutableElement> handler;

    ElementReachableAdapter(final Consumer<ExecutableElement> handler) {
        this.handler = handler;
    }

    @Override
    public void accept(CompilationContext context) {
        Facts.get(context).registerAction(Condition.when(ExecutableReachabilityFacts.IS_INVOKED), handler);
    }
}
