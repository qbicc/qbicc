package org.qbicc.plugin.opt;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EscapeAnalysis {
    private static final AttachmentKey<EscapeAnalysis> KEY = new AttachmentKey<>();
    private final List<ConnectionGraph> connectionGraphs = new CopyOnWriteArrayList<>();

    public void addConnectionGraph(ConnectionGraph connectionGraph) {
        connectionGraphs.add(connectionGraph);
    }

    boolean notEscapingMethod(Node node) {
        return escapeState(node)
            .filter(escapeState -> escapeState == EscapeState.NO_ESCAPE)
            .isPresent();
    }

    private Optional<EscapeState> escapeState(Node node) {
        return connectionGraphs.stream()
            .flatMap(cg -> cg.escapeStates.entrySet().stream())
            .filter(e -> e.getKey().equals(node))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    static EscapeAnalysis get(CompilationContext ctxt) {
        EscapeAnalysis escapeAnalysis = ctxt.getAttachment(KEY);
        if (escapeAnalysis == null) {
            escapeAnalysis = new EscapeAnalysis();
            EscapeAnalysis appearing = ctxt.putAttachmentIfAbsent(KEY, escapeAnalysis);
            if (appearing != null) {
                escapeAnalysis = appearing;
            }
        }
        return escapeAnalysis;
    }

    enum EscapeState {
        GLOBAL_ESCAPE, ARG_ESCAPE, NO_ESCAPE;

        boolean isArgEscape() {
            return this == ARG_ESCAPE;
        }
    }

    static final class ConnectionGraph {
        private final Map<Node, Value> pointsToEdges = new ConcurrentHashMap<>(); // solid (P) edges
        private final Map<Node, ValueHandle> deferredEdges = new ConcurrentHashMap<>(); // dashed (D) edges
        private final Map<Value, Set<ValueHandle>> fieldEdges = new ConcurrentHashMap<>(); // solid (F) edges
        private final Map<Node, EscapeState> escapeStates = new ConcurrentHashMap<>();

        // TODO remove when it's possible to bind a collection and add GCs into it,
        //  see https://downloads.jboss.org/byteman/4.0.16/byteman-programmers-guide.html#linkmaps
        private final ExecutableElement element;

        ConnectionGraph(ExecutableElement element) {
            this.element = element;
        }

        void setArgEscape(Node node) {
            escapeStates.put(node, EscapeState.ARG_ESCAPE);
        }

        void setGlobalEscape(Value value) {
            escapeStates.put(value, EscapeState.GLOBAL_ESCAPE);
        }

        void setNoEscape(Value value) {
            escapeStates.put(value, EscapeState.NO_ESCAPE);
        }

        public boolean addFieldEdgeIfAbsent(New from, ValueHandle to) {
            return fieldEdges
                .computeIfAbsent(from, obj -> new HashSet<>())
                .add(to);
        }

        public boolean addPointsToEdgeIfAbsent(ValueHandle from, New to) {
            return pointsToEdges.putIfAbsent(from, to) == null;
        }

        public boolean addDeferredEdgeIfAbsent(Node from, ValueHandle to) {
            return deferredEdges.putIfAbsent(from, to) == null;
        }

        void methodExit() {
            // TODO: 1. compute set of nodes reachable from GlobalEscape node(s)

            // TODO: 2. compute set of nodes reachable from ArgEscape (nodes), but not any GlobalEscape node
            escapeStates.entrySet().stream()
                .filter(e -> e.getValue().isArgEscape())
                .forEach(e -> computeArgEscapeOnly(e.getKey()));

            // TODO: 3. compute set of nodes not reachable from GlobalEscape or ArgEscape
        }

        private void computeArgEscapeOnly(Node from) {
            // TODO filter that not global reachable

            final Node to = deferredEdges.get(from) != null
                ? deferredEdges.get(from)
                : pointsToEdges.get(from);

            if (to != null) {
                setArgEscape(to);
                computeArgEscapeOnly(to);
            }
        }
    }
}
