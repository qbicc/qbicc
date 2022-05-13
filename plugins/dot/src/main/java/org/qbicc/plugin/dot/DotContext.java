package org.qbicc.plugin.dot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.definition.element.ExecutableElement;

public class DotContext {
    private final Appendable appendable;
    private final BasicBlock entryBlock;
    private final NodeVisitor<DotContext, String, String, String, String> nodeVisitor;
    private final Map<Node, String> visited = new HashMap<>();
    private final Set<BasicBlock> blockQueued = ConcurrentHashMap.newKeySet();
    private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
    private final ExecutableElement element;
    private int depth;
    private int counter;
    private int bbCounter;
    private boolean attr;
    private boolean commaNeeded;
    private Queue<String> dependencyList = new ArrayDeque();
    private final List<NodePair> bbConnections = new ArrayList<>(); // stores pair of Terminator, BlockEntry
    private final Queue<PhiValue> phiQueue = new ArrayDeque<>();

    DotContext(Appendable appendable, BasicBlock entryBlock, ExecutableElement element, CompilationContext ctxt,
               BiFunction<CompilationContext, NodeVisitor<DotContext, String, String, String, String>, NodeVisitor<DotContext, String, String, String, String>> nodeVisitorFactory
    ) {
        this.appendable = appendable;
        this.entryBlock = entryBlock;
        this.element = element;
        this.nodeVisitor = nodeVisitorFactory.apply(ctxt, new Terminus());
    }

    public ExecutableElement getElement() {
        return element;
    }

    public void appendTo(Object obj) {
        try {
            appendable.append(obj.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void attr(String name, String val) {
        if (! attr) {
            attr = true;
            appendTo(" [");
        }
        if (commaNeeded) {
            appendTo(',');
        } else {
            commaNeeded = true;
        }
        appendTo(name);
        appendTo('=');
        quote(val);
    }

    public void nl() {
        if (attr) {
            appendTo(']');
            attr = false;
            commaNeeded = false;
        }
        appendTo(System.lineSeparator());
    }

    public String getName(final Node node) {
        return visited.get(node);
    }

    String getName() {
        return "n" + (counter - 1);
    }

    BasicBlock getEntryBlock() {
        return entryBlock;
    }

    String nextName() {
        int id = counter++;
        if (id > 500) {
            throw new TooBigException();
        }
        return "n" + id;
    }

    void addVisited(final Node node, String name) {
         visited.put(node, name);
    }

    void appendTo(char c) {
        try {
            appendable.append(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void addEdge(Node from, Node to, DotAttributes attributes) {
        String fromName = visit(from);
        String toName = visit(to);
        appendTo(fromName);
        appendTo(" -> ");
        appendTo(toName);
        attr("style", attributes.style());
        attr("color", attributes.color());
        nl();
    }

    void addEdge(Node from, Value to, DotAttributes attributes, String label) {
        String fromName = visit(from);
        String toName = visit(to);
        appendTo(fromName);
        appendTo(" -> ");
        appendTo(toName);
        attr("label", label);
        attr("style", attributes.style());
        attr("color", attributes.color());
        nl();
    }

    void addBBConnection(Terminator from, BasicBlock to) {
        bbConnections.add(new NodePair(from, to.getBlockEntry()));
        addToQueue(to);
    }

    void addBBConnection(Terminator from, BasicBlock to, String label) {
        bbConnections.add(new NodePair(from, to.getBlockEntry(), label));
        addToQueue(to);
    }

    void addBBConnection(Terminator from, BasicBlock to, String label, DotAttributes style) {
        bbConnections.add(new NodePair(from, to.getBlockEntry(), label, style));
        addToQueue(to);
    }

    void addToQueue(final BasicBlock block) {
        if (blockQueued.add(block)) {
            blockQueue.add(block);
        }
    }

    void processDependency(Node node) {
        if (depth++ > 500) {
            throw new TooBigException();
        }
        try {
            visit(node);
        } finally {
            depth--;
        }
    }

    void addDependency(String name) {
        dependencyList.add(name);
    }

    void processDependencyList() {
        String name;
        while ((name = dependencyList.poll()) != null) {
            appendTo(name);
            if (!dependencyList.isEmpty()) {
                appendTo(" -> ");
            }
        }
        attr("style", DotNodeVisitor.EdgeType.ORDER_DEPENDENCY.style());
        attr("color", DotNodeVisitor.EdgeType.ORDER_DEPENDENCY.color());
        nl();
    }

    void process() {
        addToQueue(entryBlock);
        BasicBlock block;
        while ((block = blockQueue.poll()) != null) {
            String bbName = nextBBName();
            appendTo("subgraph cluster_" + bbName + " {");
            nl();
            appendTo("label = \"" + bbName + "\";");
            nl();
            visit(block.getTerminator());
        }
        connectBasicBlocks();
        processPhiQueue();
    }

    public String visit(Node node) {
        String nodeName = visited.get(node);
        if (Objects.isNull(nodeName)) {
            nodeName = register(node);

            if (node instanceof Value value) {
                value.accept(nodeVisitor, this);
            } else if (node instanceof Action action) {
                action.accept(nodeVisitor, this);
            } else if (node instanceof ValueHandle valueHandle) {
                valueHandle.accept(nodeVisitor, this);
            } else {
                assert node instanceof Terminator;
                ((Terminator) node).accept(nodeVisitor, this);
            }
        }

        return nodeName;
    }

    private void connectBasicBlocks() {
        for (NodePair pair: bbConnections) {
            assert(visited.get(pair.n1) != null);
            assert(visited.get(pair.n2) != null);
            appendTo(visited.get(pair.n1));
            appendTo(" -> ");
            appendTo(visited.get(pair.n2));
            attr("label", pair.label);
            attr("style", pair.edgeType.style());
            attr("color", pair.edgeType.color());
            nl();
        }
    }

    void addToPhiQueue(PhiValue phiValue) {
        phiQueue.add(phiValue);
    }

    private void processPhiQueue() {
        PhiValue phi;
        while ((phi = phiQueue.poll()) != null) {
            for (BasicBlock block : phi.getPinnedBlock().getIncoming()) {
                Value value = phi.getValueForInput(block.getTerminator());
                if (block.isReachable()) {
                    addEdge(phi, value, DotNodeVisitor.EdgeType.PHI_INCOMING);
                } else {
                    addEdge(phi, value, DotNodeVisitor.EdgeType.PHI_INCOMING_UNREACHABLE);
                }
            }
            addEdge(phi, phi.getPinnedBlock().getBlockEntry(), DotNodeVisitor.EdgeType.PHI_PINNED_NODE);
        }
    }

    private void quote(String orig) {
        appendTo('"');
        int cp;
        for (int i = 0; i < orig.length(); i += Character.charCount(cp)) {
            cp = orig.codePointAt(i);
            if (cp == '"') {
                appendTo('\\');
            } else if (cp == '\\') {
                if((i + 1) == orig.length() ||
                    "nlrGNTHE".indexOf(orig.codePointAt(i + 1)) == -1) {
                    appendTo('\\');
                }
            }
            if (Character.charCount(cp) == 1) {
                appendTo((char) cp);
            } else {
                appendTo(Character.highSurrogate(cp));
                appendTo(Character.lowSurrogate(cp));
            }
        }
        appendTo('"');
    }

    public String register(final Node node) {
        String name = nextName(); 
        addVisited(node, name);
        return name;
    }

    private String nextBBName() {
        int id = bbCounter++;
        if (id > 100) {
            throw new TooBigException();
        }
        return "b" + id;
    }

    record NodePair(Node n1, Node n2, String label, DotAttributes edgeType) {
        NodePair(Node n1, Node n2) {
            this(n1, n2, "");
        }

        NodePair (Node n1, Node n2, String label) {
            this(n1, n2, label, DotNodeVisitor.EdgeType.CONTROL_FLOW);
        }
    }

    private static final class Terminus implements NodeVisitor<DotContext, String, String, String, String> {
        @Override
        public String visitUnknown(DotContext param, Action node) {
            return param.visited.get(node);
        }

        @Override
        public String visitUnknown(DotContext param, Terminator node) {
            return param.visited.get(node);
        }

        @Override
        public String visitUnknown(DotContext param, ValueHandle node) {
            return param.visited.get(node);
        }

        @Override
        public String visitUnknown(DotContext param, Value node) {
            return param.visited.get(node);
        }
    }
}
