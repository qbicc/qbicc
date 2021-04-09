package org.qbicc.type.generic;

import java.util.NoSuchElementException;

import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.ParameterElement;
import io.smallrye.common.constraint.Assert;

/**
 * A context in which a type parameter can be resolved by name.
 */
public interface TypeParameterContext {

    /**
     * Get the enclosing type parameter context.  All contexts are ultimately enclosed by {@link #EMPTY}.
     *
     * @return the enclosing type parameter context (not {@code null})
     */
    default TypeParameterContext getEnclosingTypeParameterContext() {
        return EMPTY;
    }

    /**
     * Resolve a type parameter name to a type parameter.
     *
     * @param parameterName the type parameter name (must not be {@code null})
     * @return the type parameter (not {@code null})
     * @throws NoSuchElementException if the type parameter is not found in any enclosing context
     */
    TypeParameter resolveTypeParameter(String parameterName) throws NoSuchElementException;

    /**
     * Create a type parameter context based on an enclosing context and a signature.
     *
     * @param enclosing the enclosing context (must not be {@code null})
     * @param signature the signature (must not be {@code null})
     * @return the new context (not {@code null})
     */
    static TypeParameterContext create(TypeParameterContext enclosing, ParameterizedSignature signature) {
        Assert.checkNotNullParam("enclosing", enclosing);
        Assert.checkNotNullParam("signature", signature);
        return parameterName -> {
            TypeParameter typeParameter = signature.getTypeParameter(parameterName);
            return typeParameter != null ? typeParameter : enclosing.resolveTypeParameter(parameterName);
        };
    }

    /**
     * Get the given element's type parameter context.  Methods and constructors have their own context; all other
     * elements use the context of the enclosing type.
     *
     * @param element the element (must not be {@code null})
     * @return the context (not {@code null})
     */
    static TypeParameterContext of(Element element) {
        Assert.checkNotNullParam("element", element);
        if (element instanceof InvokableElement) {
            return (InvokableElement) element;
        } else if (element instanceof LocalVariableElement || element instanceof ParameterElement) {
            throw Assert.unsupported();
        } else {
            return element.getEnclosingType();
        }
    }

    /**
     * The empty type parameter context.
     */
    TypeParameterContext EMPTY = parameterName -> {
        throw new NoSuchElementException("Type parameter " + parameterName + " cannot be resolved");
    };
}
