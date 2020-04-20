package cc.quarkus.qcc.graph.node;

import java.util.List;
import java.util.stream.Collectors;

import javax.naming.ldap.Control;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public interface Node<T extends Type<T>, V extends Value<T,V>> {

    int getId();

    ControlNode<?, ?> getControl();

    void setControl(ControlNode<?, ?> control);

    void addSuccessor(Node<?, ?> out);

    V getValue(Context context);

    T getType();

    List<? extends Node<?, ?>> getPredecessors();

    default List<? extends ControlNode<?,?>> getControlPredecessors() {
        return getPredecessors().stream()
                .filter(e-> e instanceof ControlNode<?,?> )
                .map(e->(ControlNode<?,?>)e)
                .collect(Collectors.toList());
    }

    List<? extends Node<?, ?>> getSuccessors();

    default List<? extends ControlNode<?, ?>> getControlSuccessors() {
        return getSuccessors().stream()
                .filter(e-> e instanceof ControlNode<?,?> )
                .map(e->(ControlNode<?,?>)e)
                .collect(Collectors.toList());
    }

}
