package cc.quarkus.qcc.interpret;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartToken;

public class Thread implements Context {

    public Thread(Heap heap) {
        this.heap = heap;
    }


    public EndToken execute(Graph graph, StartToken arguments) {
        //System.err.println( ">> CALL " + graph.getMethod());
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
        //System.err.println( "<< CALL " + graph.getMethod() + " = " + this.endToken);
        return this.endToken;
    }

    protected ControlNode<?> executeControl(ControlNode<?> prevControl, ControlNode<?> control) {
        if (control instanceof RegionNode) {
            hydratePhis(prevControl, (RegionNode) control);
        }

        List<Node<?>> worklist = new ArrayList<>(control.getSuccessors());

        ControlNode<?> nextControl = null;

        while (!worklist.isEmpty()) {
            Deque<Node<?>> ready = worklist.stream().filter(e -> isReady(prevControl, control, e)).collect(Collectors.toCollection(ArrayDeque::new));
            worklist.removeAll(ready);

            while (!ready.isEmpty()) {
                Node<?> cur = ready.pop();
                Object value = cur.getValue(peekContext());
                //System.err.println( "EXECUTE: " + control + " :: " + cur + " = " + value);
                if (value != null) {
                    if (cur instanceof EndNode) {
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

    protected void hydratePhis(ControlNode<?> discriminator, RegionNode region) {
        region.getSuccessors().stream().filter(e -> e instanceof PhiNode).forEach(phi -> {
            Node<?> v = ((PhiNode<?>) phi).getValue(discriminator);
            set0(phi, v.getValue(peekContext()));
        });
    }

    @SuppressWarnings("unchecked")
    protected <T> T getValue(ControlNode<?> discriminator, Node<T> node) {
        if (node instanceof PhiNode) {
            return (T) ((PhiNode<?>) node).getValue(discriminator).getValue(peekContext());
        }
        return node.getValue(peekContext());
    }

    protected boolean isReady(ControlNode<?> discriminator, ControlNode<?> curControl, Node<?> node) {
        if (node instanceof RegionNode) {
            return isRegionReady(curControl, (RegionNode) node);
        }
        if (node instanceof PhiNode) {
            return isPhiReady(discriminator, (PhiNode<?>) node);
        }
        Optional<? extends Node<?>> firstMissing = node.getPredecessors().stream().filter(e -> get(e) == null).findFirst();
        return firstMissing.isEmpty();
    }

    protected boolean isRegionReady(ControlNode<?> discriminator, RegionNode node) {
        Optional<? extends Node<?>> firstFound = node.getPredecessors().stream().filter(e -> get(e) != null).findFirst();
        if (firstFound.isEmpty()) {
            return false;
        }
        return node.getSuccessors().stream().filter(e -> e instanceof PhiNode).allMatch(e -> isPhiReady(discriminator, (PhiNode<?>) e));
    }

    protected boolean isPhiReady(ControlNode<?> discriminator, PhiNode<?> node) {
        boolean result = peekContext().get(node.getValue(discriminator)) != null;
        return result;
    }

    protected void pushContext() {
        this.callStack.push(new StackFrame(heap()));
    }

    protected void popContext() {
        this.callStack.pop();
    }

    protected StackFrame peekContext() {
        return this.callStack.peek();
    }


    @Override
    public <T> void set(Node<T> node, T val) {
        peekContext().set(node, val);
    }

    @SuppressWarnings("unchecked")
    protected <T> void set0(Node<T> node, Object val) {
        set(node, (T) val);
    }

    @Override
    public <T> T get(Node<T> node) {
        return peekContext().get(node);
    }

    @Override
    public Heap heap() {
        return this.heap;
    }

    private Deque<StackFrame> callStack = new ArrayDeque<>();

    private EndToken endToken;

    private Heap heap;
}
