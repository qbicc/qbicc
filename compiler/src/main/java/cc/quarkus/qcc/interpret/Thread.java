package cc.quarkus.qcc.interpret;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartValue;

public class Thread implements Context {

    public Thread() {

    }


    public  EndToken execute(Graph graph, StartValue arguments) {
        pushContext();
        try {
            set(graph.getStart(), arguments);
            ControlNode<?> prevControl = null;
            ControlNode<?> nextControl = null;
            ControlNode<?> control = graph.getStart();
            do {
                nextControl = executeControl(prevControl, control);
                prevControl = control;
                control = nextControl;
            } while (control != null);
        } finally {
            popContext();
        }
        return this.endToken;
    }

    protected ControlNode<?> executeControl(ControlNode<?> prevControl, ControlNode<?> control) {
        List<Node<?>> worklist = new ArrayList<>(control.getSuccessors());

        ControlNode<?> nextControl = null;

        while ( ! worklist.isEmpty() ) {
            Deque<Node<?>> ready = new ArrayDeque<>(worklist.stream().filter(e->isReady(prevControl, control, e)).collect(Collectors.toList()));
            worklist.removeAll(ready);

            while ( ! ready.isEmpty() ) {
                Node<?> cur = ready.pop();
                Object value = getValue(prevControl, cur);
                if (value != null) {
                    if ( value instanceof EndToken ) {
                        this.endToken = (EndToken) value;
                        return null;
                    }
                    set0(cur, value);
                    if (cur instanceof ControlNode) {
                        nextControl = (ControlNode<?>) cur;
                    }
                }
            }
        }
        return nextControl;
    }

    protected <T> T getValue(ControlNode<?> discriminator, Node<T> node) {
        if ( node instanceof PhiNode ) {
            return (T) ((PhiNode<?>) node).getValue(discriminator).getValue(peekContext());
        }
        return node.getValue(peekContext());
    }

    protected boolean isReady(ControlNode<?> discriminator, ControlNode<?> curControl, Node<?> node) {
        if ( node instanceof RegionNode ) {
            return isRegionReady(curControl, (RegionNode) node);
        }
        if ( node instanceof PhiNode ) {
            return isPhiReady(discriminator, (PhiNode<?>) node);
        }
        Optional<? extends Node<?>> firstMissing = node.getPredecessors().stream().filter(e -> get(e) == null).findFirst();
        return ! firstMissing.isPresent();
    }

    protected boolean isRegionReady(ControlNode<?> discriminator, RegionNode node) {
        Optional<? extends Node<?>> firstFound = node.getPredecessors().stream().filter(e -> get(e) != null).findFirst();
        if ( ! firstFound.isPresent() ) {
            return false;
        }
        return node.getSuccessors().stream().filter(e->e instanceof PhiNode).allMatch(e->isPhiReady(discriminator, (PhiNode<?>) e));
    }

    protected boolean isPhiReady(ControlNode<?> discriminator, PhiNode<?> node) {
        return peekContext().get(node.getValue(discriminator)) != null;
        //return false;
    }

    protected void pushContext() {
        this.callStack.push( new ThreadContext());
    }

    protected void popContext() {
        this.callStack.pop();
    }

    protected ThreadContext peekContext() {
        return this.callStack.peek();
    }


    @Override
    public <T> void set(Node<T> node, T val) {
        peekContext().set(node, val);
    }

    protected <T> void set0(Node<T> node, Object val) {
        set(node, (T) val);
    }

    @Override
    public <T> T get(Node<T> node) {
        return peekContext().get(node);
    }

    private Deque<ThreadContext> callStack = new ArrayDeque<>();
    private EndToken endToken;
}
