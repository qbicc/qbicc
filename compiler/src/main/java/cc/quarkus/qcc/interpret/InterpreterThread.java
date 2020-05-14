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
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartToken;
import cc.quarkus.qcc.type.QType;

public class InterpreterThread implements Context {

    public InterpreterThread(InterpreterHeap heap) {
        this.heap = heap;
    }


    public <V extends QType> EndToken<V> execute(Graph<V> graph, Object...arguments) {
        return execute(graph, new StartToken(arguments));
    }

    public <V extends QType> EndToken<V> execute(Graph<V> graph, List<Object> arguments) {
        return execute(graph, arguments.toArray());
    }

    @SuppressWarnings("unchecked")
    public <V extends QType> EndToken<V> execute(Graph<V> graph, StartToken arguments) {
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
        return (EndToken<V>) this.endToken;
    }

    protected ControlNode<?> executeControl(ControlNode<?> prevControl, ControlNode<?> control) {
        if (control instanceof RegionNode) {
            hydratePhis(prevControl, (RegionNode) control);
        }

        List<Node<?>> worklist = new ArrayList<>(control.getSuccessors());

        ControlNode<?> nextControl = null;

        while (!worklist.isEmpty()) {
            Deque<Node<?>> ready = worklist.stream().filter(e -> isReady(prevControl, control, e)).collect(Collectors.toCollection(ArrayDeque::new));
            if (ready.isEmpty()) {
                throw new RuntimeException("deadlock");
            }

            worklist.removeAll(ready);

            while (!ready.isEmpty()) {
                Node<?> cur = ready.pop();
                //System.err.println("PREP: " + control + " :: " + cur);
                Object value = cur.getValue(peekContext());
                //System.err.println(" V=" + value);
                //System.err.println(" V'=" + value);

                if (cur instanceof EndNode) {
                    this.endToken = (EndToken<?>) value;
                    return null;
                }
                set0(cur, value);
                if (cur instanceof ControlNode && value != ControlToken.NO_CONTROL) {
                    nextControl = (ControlNode<?>) cur;
                }
            }
        }
        return nextControl;
    }

    protected void hydratePhis(ControlNode<?> discriminator, RegionNode region) {
        region.getSuccessors().stream().filter(e -> e instanceof PhiNode).forEach(phi -> {
            //System.err.println( "** hydrate: " + phi);
            Node<?> v = ((PhiNode<?>) phi).getValue(discriminator);
            set0(phi, v.getValue(peekContext()));
        });
    }

    @SuppressWarnings("unchecked")
    protected <T extends QType> T getValue(ControlNode<?> discriminator, Node<T> node) {
        if (node instanceof PhiNode) {
            return (T) ((PhiNode<?>) node).getValue(discriminator).getValue(peekContext());
        }
        return node.getValue(peekContext());
    }

    protected boolean isReady(ControlNode<?> discriminator, ControlNode<?> curControl, Node<?> node) {
        //System.err.println("isReady? d=" + discriminator + " c=" + curControl + " n=" + node + " pred=" + node.getPredecessors());
        if (node instanceof RegionNode) {
            return isRegionReady(curControl, (RegionNode) node);
        }
        if (node instanceof PhiNode) {
            return isPhiReady(discriminator, (PhiNode<?>) node);
        }
        Optional<? extends Node<?>> firstMissing = node.getPredecessors().stream().filter(e -> ! contains(e) ).findFirst();
        //if (firstMissing.isPresent()) {
            //System.err.println(" missing: " + firstMissing.get() + " controlled " + firstMissing.get().getControl());
        //}
        return firstMissing.isEmpty();
    }

    protected boolean isRegionReady(ControlNode<?> discriminator, RegionNode node) {
        Optional<? extends Node<?>> firstFound = node.getPredecessors().stream().filter(this::contains).findFirst();
        if (firstFound.isEmpty()) {
            return false;
        }
        return node.getSuccessors().stream().filter(e -> e instanceof PhiNode).allMatch(e -> isPhiReady(discriminator, (PhiNode<?>) e));
    }

    protected boolean isPhiReady(ControlNode<?> discriminator, PhiNode<?> node) {
        boolean result = peekContext().contains(node.getValue(discriminator));
        return result;
    }

    protected void pushContext() {
        this.callStack.push(new StackFrame(thread()));
    }

    protected void popContext() {
        this.callStack.pop();
    }

    protected StackFrame peekContext() {
        return this.callStack.peek();
    }


    @Override
    public <T extends QType> void set(Node<T> node, T val) {
        peekContext().set(node, val);
    }

    @SuppressWarnings("unchecked")
    protected <T extends QType> void set0(Node<T> node, Object val) {
        set(node, (T) val);
    }

    @Override
    public <T extends QType> T get(Node<T> node) {
        return peekContext().get(node);
    }

    public boolean contains(Node<?> node) {
        return peekContext().contains(node);
    }

    @Override
    public InterpreterThread thread() {
        return this;
    }

    public InterpreterHeap heap() {
        return this.heap;
    }

    private Deque<StackFrame> callStack = new ArrayDeque<>();

    private EndToken<?> endToken;

    private InterpreterHeap heap;
}
