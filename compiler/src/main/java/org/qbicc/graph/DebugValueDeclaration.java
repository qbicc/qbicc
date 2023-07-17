package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 * A node which establishes that, at this point of the program, the given variable has the given value.
 */
public final class DebugValueDeclaration extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final LocalVariableElement variable;
    private final Value value;

    DebugValueDeclaration(final ProgramLocatable pl, Node dependency, LocalVariableElement variable, Value value) {
        super(pl);
        this.dependency = dependency;
        this.variable = variable;
        this.value = value;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public LocalVariableElement getVariable() {
        return variable;
    }

    public Value getValue() {
        return value;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(dependency, variable, value);
    }

    @Override
    String getNodeName() {
        return "DebugValue";
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? value : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DebugValueDeclaration && equals((DebugValueDeclaration) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        value.getType().toString(b);
        b.append(' ');
        b.append(variable.getName());
        b.append(')');
        b.append('@');
        value.toReferenceString(b);
        return b;
    }

    public boolean equals(DebugValueDeclaration other) {
        return this == other || other != null && dependency.equals(other.dependency) && variable.equals(other.variable) && value.equals(other.value);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
