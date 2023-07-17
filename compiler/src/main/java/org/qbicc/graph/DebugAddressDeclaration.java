package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 * A node which establishes that, at this point of the program, the given variable has the given address.
 */
public final class DebugAddressDeclaration extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final LocalVariableElement variable;
    private final Value address;

    DebugAddressDeclaration(final ProgramLocatable pl, Node dependency, LocalVariableElement variable, Value address) {
        super(pl);
        this.dependency = dependency;
        this.variable = variable;
        this.address = address;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public LocalVariableElement getVariable() {
        return variable;
    }

    public Value getAddress() {
        return address;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(dependency, variable, address);
    }

    @Override
    String getNodeName() {
        return "DebugAddress";
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? address : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DebugAddressDeclaration && equals((DebugAddressDeclaration) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        address.getPointeeType().toString(b);
        b.append(' ');
        b.append(variable.getName());
        b.append(')');
        b.append('@');
        address.toReferenceString(b);
        return b;
    }

    public boolean equals(DebugAddressDeclaration other) {
        return this == other || other != null && dependency.equals(other.dependency) && variable.equals(other.variable) && address.equals(other.address);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
