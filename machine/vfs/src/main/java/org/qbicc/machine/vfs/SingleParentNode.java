package org.qbicc.machine.vfs;

abstract class SingleParentNode extends Node {

    SingleParentNode() {}

    public abstract Node getParent();

    public abstract boolean changeParent(Node oldParent, Node newParent);
}
