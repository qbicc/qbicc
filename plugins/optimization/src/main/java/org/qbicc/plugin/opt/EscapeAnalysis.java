package org.qbicc.plugin.opt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.Executable;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.definition.element.ExecutableElement;

public class EscapeAnalysis {
    private static final AttachmentKey<EscapeAnalysis> KEY = new AttachmentKey<>();
    private final Map<ExecutableElement, ConnectionGraph> connectionGraphs = new HashMap<>();
    final CallGraph callGraph = new CallGraph();

    void addConnectionGraph(ConnectionGraph connectionGraph, ExecutableElement element) {
        connectionGraphs.put(element, connectionGraph);
    }

    boolean isNotEscapingMethod(New new_, ExecutableElement element) {
        final ConnectionGraph connectionGraph = connectionGraphs.get(element);
        return connectionGraph != null && EscapeState.isNoEscape(connectionGraph.escapeStates.get(new_));
    }

    public static void interProcedureAnalysis(CompilationContext ctxt) {
        // TODO run it in parallel?
        final EscapeAnalysis escapeAnalysis = EscapeAnalysis.get(ctxt);
        final Set<ExecutableElement> visited = new HashSet<>();
        escapeAnalysis.connectionGraphs.keySet().forEach(element -> updateConnectionGraphIfNotVisited(element, escapeAnalysis, visited));
    }

    /**
     * Traversal reverse topological order over the program control flow.
     * A bottom-up traversal in which the connection graph of a callee
     * is used to update the connection graph of the caller.
     */
    private static ConnectionGraph updateConnectionGraphIfNotVisited(ExecutableElement caller, EscapeAnalysis escapeAnalysis, Set<ExecutableElement> visited) {
        if (!visited.add(caller)) {
            return escapeAnalysis.connectionGraphs.get(caller);
        }

        return updateConnectionGraph(caller, escapeAnalysis, visited);
    }

    private static ConnectionGraph updateConnectionGraph(ExecutableElement caller, EscapeAnalysis escapeAnalysis, Set<ExecutableElement> visited) {
        final ConnectionGraph callerCG = escapeAnalysis.connectionGraphs.get(caller);
        final List<Call> callees = escapeAnalysis.callGraph.calls.get(caller);
        if (callees == null) {
            return callerCG;
        }

        for (Call callee : callees) {
            final ExecutableElement calleeElement = ((Executable) callee.getValueHandle()).getExecutable();
            final ConnectionGraph calleeCG = updateConnectionGraphIfNotVisited(calleeElement, escapeAnalysis, visited);
            if (calleeCG != null) {
                updateCallerNodes(caller, callee, calleeCG, escapeAnalysis);
            }
        }

        return callerCG;
    }

    private static void updateCallerNodes(ExecutableElement caller, Call callee, ConnectionGraph calleeCG, EscapeAnalysis escapeAnalysis) {
        // TODO caller connection graph lookup should be hoisted out of the loop,
        //      keeping it here for convenience with byteman based logging
        final ConnectionGraph callerCG = escapeAnalysis.connectionGraphs.get(caller);

        final List<Value> arguments = callee.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            final Value outsideArg = arguments.get(i);
            final ParameterValue insideArg = calleeCG.arguments.get(i);

            final Collection<Node> mapsToNode = new ArrayList<>();
            mapsToNode.add(outsideArg);

            updateNodes(insideArg, mapsToNode, calleeCG, callerCG);
        }
    }

    private static void updateNodes(Node calleeNode, Collection<Node> mapsToNode, ConnectionGraph calleeCG, ConnectionGraph callerCG) {
        for (Value calleePointed : calleeCG.pointsTo(calleeNode)) {
            for (Node callerNode : mapsToNode) {
                final Collection<Value> allCallerPointed = callerCG.pointsTo(callerNode);
                if (allCallerPointed.isEmpty()) {
                    // TODO do I need to insert a new node as the target for callerNode?
                }

                for (Value callerPointed : allCallerPointed) {
                    if (mapsToNode.add(calleePointed)) {
                        // The escape state of caller nodes is marked GlobalEscape,
                        // if the escape state of the callee node is GlobalEscape.
                        callerCG.mergeEscapeState(callerPointed, calleeCG.escapeStates.get(calleePointed));

                        for (InstanceFieldOf calleeField : calleeCG.fieldEdges.get(calleePointed)) {
                            final String calleeFieldName = calleeField.getVariableElement().getName();
                            final Collection<Node> callerField = callerCG.fieldEdges.get(callerPointed).stream()
                                .filter(field -> Objects.equals(field.getVariableElement().getName(), calleeFieldName))
                                .collect(Collectors.toList());

                            updateNodes(calleeField, callerField, calleeCG, callerCG);
                        }
                    }
                }
            }
        }

        // final Collection<Value> pointed = calleeCG.pointsTo(node);
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

        boolean isGlobalEscape() {
            return this == GLOBAL_ESCAPE;
        }

        boolean isNoEscape() {
            return this == NO_ESCAPE;
        }

        EscapeState merge(EscapeState other) {
            if (other.isGlobalEscape())
                return GLOBAL_ESCAPE;

            return this;
        }

        static boolean isNoEscape(EscapeState escapeState) {
            return escapeState != null && escapeState.isNoEscape();
        }
    }

    // TODO override toString() and show the name of the method for which this connection graph is set
    static final class ConnectionGraph {
        private final Map<Node, Value> pointsToEdges = new HashMap<>(); // solid (P) edges
        private final Map<Node, ValueHandle> deferredEdges = new HashMap<>(); // dashed (D) edges
        private final Map<Value, Collection<InstanceFieldOf>> fieldEdges = new HashMap<>(); // solid (F) edges
        private final Map<Node, EscapeState> escapeStates = new HashMap<>();

        private final List<ParameterValue> arguments = new ArrayList<>();
        private Value returnValue;

        /**
         * PointsTo(p) returns the set of of nodes that are immediately pointed by p.
         */
        Collection<Value> pointsTo(Node node) {
            final ValueHandle deferredEdge = deferredEdges.get(node);
            if (deferredEdge != null) {
                final Value pointedByDeferred = pointsToEdges.get(deferredEdge);
                if (pointedByDeferred != null)
                    return List.of(pointedByDeferred);
            }

            final Value pointedDirectly = pointsToEdges.get(node);
            if (pointedDirectly != null) {
                return List.of(pointedDirectly);
            }

            return Collections.emptyList();
        }

        void addParameters(List<ParameterValue> args) {
            arguments.addAll(args);
            arguments.forEach(arg -> this.setEscape(arg, EscapeState.ARG_ESCAPE));
        }

        void addReturn(Value value) {
            returnValue = value;
            setEscape(value, EscapeState.ARG_ESCAPE);
        }

        void setEscape(Node node, EscapeState escapeState) {
            escapeStates.put(node, escapeState);
        }

        boolean mergeEscapeState(Node node, EscapeState otherEscapeState) {
            final EscapeState escapeState = escapeStates.get(node);
            final EscapeState mergedEscapeState = escapeState.merge(otherEscapeState);
            return escapeStates.replace(node, escapeState, mergedEscapeState);
        }

        public boolean addFieldEdgeIfAbsent(New from, InstanceFieldOf to) {
            return fieldEdges
                .computeIfAbsent(from, obj -> new ArrayList<>())
                .add(to);
        }

        public boolean addPointsToEdgeIfAbsent(ValueHandle from, New to) {
            return pointsToEdges.putIfAbsent(from, to) == null;
        }

        public boolean addDeferredEdgeIfAbsent(Node from, ValueHandle to) {
            return deferredEdges.putIfAbsent(from, to) == null;
        }

        void methodExit() {
            // TODO: Use ByPass function to eliminate all deferred edges in the CG

            // TODO: 1. compute set of nodes reachable from GlobalEscape node(s)

            // TODO: 2. compute set of nodes reachable from ArgEscape (nodes), but not any GlobalEscape node
            final List<Node> argEscapeOnly = escapeStates.entrySet().stream()
                .filter(e -> e.getValue().isArgEscape())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            // Separate computing from filtering since it modifies the collection itself
            argEscapeOnly.forEach(this::computeArgEscapeOnly);

            // TODO: 3. compute set of nodes not reachable from GlobalEscape or ArgEscape
        }

        private void computeArgEscapeOnly(Node from) {
            // TODO filter that not global reachable

            final Node to = deferredEdges.get(from) != null
                ? deferredEdges.get(from)
                : pointsToEdges.get(from);

            if (to != null) {
                setEscape(to, EscapeState.ARG_ESCAPE);
                computeArgEscapeOnly(to);
            }
        }
    }

    static final class CallGraph {
        private final Map<ExecutableElement, List<Call>> calls = new ConcurrentHashMap<>();

        void calls(ExecutableElement from, Call to) {
            calls.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        }
    }
}
