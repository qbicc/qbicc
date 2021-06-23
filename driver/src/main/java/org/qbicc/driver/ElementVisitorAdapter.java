package org.qbicc.driver;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.element.ElementVisitor;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class ElementVisitorAdapter implements Consumer<ExecutableElement> {
    private final ElementVisitor<CompilationContext, Void> visitor;

    public ElementVisitorAdapter(ElementVisitor<CompilationContext, Void> visitor) {
        this.visitor = visitor;
    }

    @Override
    public void accept(ExecutableElement executableElement) {
        executableElement.accept(visitor, executableElement.getEnclosingType().getContext().getCompilationContext());
    }
}
