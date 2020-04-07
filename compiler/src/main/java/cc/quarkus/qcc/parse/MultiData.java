package cc.quarkus.qcc.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.type.AnyType;
import cc.quarkus.qcc.graph.type.Type;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class MultiData<T extends Type> {

    public MultiData(ControlNode<?> control) {
        this.control = control;
    }

    public Node<T> load(T type) {
        if (!this.kill) {
            if (this.ue == null) {
                this.ue = createPhi(type);
                this.exitValue = this.ue;
            }
            return checkType(this.ue, type);
        }
        System.err.println(this.control + " vs " + this.value);
        //return checkType(this.value.get(this.control), type);
        return checkType(this.exitValue, type);
    }

    public void store(Node<T> val) {
        this.type = this.type.join(val.getType());
        //this.value.put(this.control, val);
        this.value.add( new Entry(this.control, this.control, val));
        this.kill = true;
        this.exitValue = val;
    }

    public void merge(MultiData<T> other) {
        System.err.println( "merge local--->" + other + " into " + this);
        this.type = this.type.join(other.type);
        //Node<ConcreteType<?>> foreign = other.load(this.type);
        //this.value.put(other.control, foreign);
        //this.value.addAll(other.value);

        for (Entry<T> entry : other.value) {
            System.err.println( "merge local: " + other.control + "/" + entry.definition + "/" + entry.value);
            this.value.add( new Entry<>(other.control, entry.definition, entry.value ));
        }
        //if ( this.ue != null ) {
            //this.ue.addPredecessor(entry.value);
        //}

        //if (this.ue != null) {
            //this.ue.addPredecessor(foreign);
        //}
    }

    public void possiblySimplify() {
        if (this.ue != null) {
            this.ue.possiblySimplify();
        }
    }

    protected PhiNode<T> createPhi(T type) {
        PhiNode<T> phi = new PhiNode<>(this.control, type, this);
        for (Node<T> each : values()) {
            phi.addPredecessor(each);
        }
        return phi;
    }

    public String toString() {
        return "local: " + type + " => " + value;
    }

    public Set<Node<T>> values() {
        return this.value.stream()
                .map(e->e.value)
                .collect(Collectors.toSet());
    }

    private final ControlNode<?> control;

    private Type type = AnyType.INSTANCE;

    //private Map<ControlNode<?>, Node<? extends ConcreteType<?>>> value = new HashMap<>();

    private List<Entry<T>> value = new ArrayList<>();

    private Node<?> exitValue = null;

    private PhiNode<?> ue = null;

    private boolean kill = false;

    private static class Entry<T extends Type> {

        Entry(ControlNode<?> discriminator, ControlNode<?> definition, Node<T> value) {
            this.discriminator = discriminator;
            this.definition = definition;
            this.value = value;
        }

        final ControlNode<?> discriminator;

        final ControlNode<?> definition;

        final Node<T> value;
    }

}
