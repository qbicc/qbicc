package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.Executable;
import org.qbicc.graph.New;
import org.qbicc.type.definition.element.ExecutableElement;

public final class EscapeAnalysisState {
    private static final AttachmentKey<EscapeAnalysisState> KEY = new AttachmentKey<>();
    private final Map<ExecutableElement, List<Call>> callGraph = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, ConnectionGraph> connectionGraphs = new ConcurrentHashMap<>();

    ConnectionGraph getConnectionGraph(ExecutableElement element) {
        return connectionGraphs.get(element);
    }

    void trackMethod(ExecutableElement element, ConnectionGraph connectionGraph) {
        connectionGraphs.put(element, connectionGraph);
    }

    void trackCall(ExecutableElement from, Call to) {
        callGraph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    boolean isNotEscapingMethod(New new_, ExecutableElement element) {
        final ConnectionGraph connectionGraph = connectionGraphs.get(element);
        return connectionGraph != null && EscapeValue.isNoEscape(connectionGraph.getEscapeValue(new_));
    }

    static EscapeAnalysisState get(CompilationContext ctxt) {
        EscapeAnalysisState escapeAnalysisState = ctxt.getAttachment(KEY);
        if (escapeAnalysisState == null) {
            escapeAnalysisState = new EscapeAnalysisState();
            EscapeAnalysisState appearing = ctxt.putAttachmentIfAbsent(KEY, escapeAnalysisState);
            if (appearing != null) {
                escapeAnalysisState = appearing;
            }
        }
        return escapeAnalysisState;
    }

    public static void interMethodAnalysis(CompilationContext ctxt) {
        EscapeAnalysisState state = EscapeAnalysisState.get(ctxt);
        final Set<ExecutableElement> visited = new HashSet<>();
        state.connectionGraphs.keySet().forEach(element -> updateConnectionGraphIfNotVisited(element, visited, state));
    }

    /**
     * Traversal reverse topological order over the program control flow.
     * A bottom-up traversal in which the connection graph of a callee
     * is used to update the connection graph of the caller.
     */
    private static ConnectionGraph updateConnectionGraphIfNotVisited(ExecutableElement caller, Set<ExecutableElement> visited, EscapeAnalysisState state) {
        if (!visited.add(caller)) {
            return state.connectionGraphs.get(caller);
        }

        return updateConnectionGraph(caller, visited, state);
    }

    private static ConnectionGraph updateConnectionGraph(ExecutableElement caller, Set<ExecutableElement> visited, EscapeAnalysisState state) {
        final ConnectionGraph callerCG = state.connectionGraphs.get(caller);
        if (callerCG == null) {
            return null;
        }

        final List<Call> callees = state.callGraph.get(caller);
        if (callees == null) {
            return callerCG;
        }

        for (Call callee : callees) {
            final ExecutableElement calleeElement = ((Executable) callee.getValueHandle()).getExecutable();
            final ConnectionGraph calleeCG = updateConnectionGraphIfNotVisited(calleeElement, visited, state);
            if (calleeCG != null) {
                callerCG.update(callee, calleeCG);
            }
        }

        return callerCG;
    }
}
