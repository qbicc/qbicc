package org.qbicc.object;

import java.util.Objects;

/**
 * A global constructor or destructor registered to a program module.
 */
public class GlobalXtor {
    private final Kind kind;
    private final Function function;
    private final int priority;

    GlobalXtor(Kind kind, Function function, int priority) {
        this.kind = kind;
        this.function = function;
        this.priority = priority;
    }

    public Kind getKind() {
        return kind;
    }

    public Function getFunction() {
        return function;
    }

    public int getPriority() {
        return priority;
    }

    public enum Kind {
        CTOR("constructor"),
        DTOR("destructor"),
        ;

        private final String label;

        Kind(final String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Override
    public int hashCode() {
        return priority * 19 + Objects.hash(kind, function);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GlobalXtor gx && equals(gx);
    }

    public boolean equals(GlobalXtor other) {
        return this == other || other != null
            && kind == other.kind
            && function.equals(other.function)
            && priority == other.priority;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder b) {
        return b.append(kind.getLabel()).append(' ').append(function.toString()).append('(').append(priority).append(')');
    }
}
