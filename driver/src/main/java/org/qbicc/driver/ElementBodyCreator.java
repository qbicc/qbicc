package org.qbicc.driver;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Condition;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An element consumer which creates the element body, if any.
 */
public final class ElementBodyCreator implements Consumer<ExecutableElement> {
    /**
     * Construct a new instance.
     */
    public ElementBodyCreator() {
    }

    @Override
    public void accept(ExecutableElement executableElement) {
        executableElement.tryCreateMethodBody();
    }

    public static void register(final CompilationContext ctxt) {
        Facts.get(ctxt).registerInlineAction(Condition.when(ExecutableReachabilityFacts.IS_INVOKED), (ee, facts) -> facts.discover(ee, ExecutableReachabilityFacts.NEEDS_COMPILATION));
        Facts.get(ctxt).registerAction(Condition.when(ExecutableReachabilityFacts.NEEDS_COMPILATION), (executableElement, facts) -> executableElement.tryCreateMethodBody());
    }
}
