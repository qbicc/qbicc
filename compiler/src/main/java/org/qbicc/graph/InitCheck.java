package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.definition.element.InitializerElement;

public class InitCheck extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final InitializerElement initializerElement;
    private final Value initThunk;

    InitCheck(final ProgramLocatable pl, final Node dependency, final InitializerElement initializerElement, final Value initThunk) {
        super(pl);
        this.dependency = dependency;
        this.initializerElement = initializerElement;
        this.initThunk = initThunk;
    }

    public InitializerElement getInitializerElement() {
        return initializerElement;
    }

    public Value getInitThunk() {
        return initThunk;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? initThunk : Util.throwIndexOutOfBounds(index);
    }

    int calcHashCode() {
        return Objects.hash(InitCheck.class, dependency, initializerElement, initThunk);
    }

    @Override
    String getNodeName() {
        return "InitCheck";
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return true;
    }

    public boolean equals(Object other) {
        return other instanceof InitCheck ic && equals(ic);
    }

    public boolean equals(final InitCheck other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && initializerElement.equals(other.initializerElement)
            && initThunk.equals(other.initThunk);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        b.append(initializerElement);
        b.append(',');
        initThunk.toReferenceString(b);
        b.append(')');
        return b;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
