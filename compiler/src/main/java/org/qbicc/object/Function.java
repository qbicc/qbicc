package org.qbicc.object;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A function definition.
 */
public final class Function extends SectionObject {
    private volatile MethodBody body;
    private volatile FunctionDeclaration declaration;

    Function(final ExecutableElement originalElement, final String name, final SymbolLiteral literal) {
        super(originalElement, name, literal);
    }

    public ExecutableElement getOriginalElement() {
        return (ExecutableElement) super.getOriginalElement();
    }

    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

    public MethodBody getBody() {
        return body;
    }

    public void replaceBody(final MethodBody body) {
        this.body = body;
    }

    public FunctionDeclaration getDeclaration() {
        FunctionDeclaration declaration = this.declaration;
        if (declaration == null) {
            synchronized (this) {
                declaration = this.declaration;
                if (declaration == null) {
                    declaration = this.declaration = new FunctionDeclaration(originalElement, name, literal);
                }
            }
        }
        return declaration;
    }
}
