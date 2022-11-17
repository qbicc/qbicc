package org.qbicc.graph.literal;

import org.qbicc.type.definition.element.InvokableElement;

/**
 *
 */
public abstract sealed class InvokableLiteral extends ExecutableLiteral permits ConstructorLiteral, FunctionLiteral, MethodLiteral {
    InvokableLiteral(InvokableElement element) {
        super(element);
    }

    @Override
    public final boolean equals(ExecutableLiteral other) {
        return other instanceof InvokableLiteral il && equals(il);
    }

    public boolean equals(InvokableLiteral other) {
        return super.equals(other);
    }
}
