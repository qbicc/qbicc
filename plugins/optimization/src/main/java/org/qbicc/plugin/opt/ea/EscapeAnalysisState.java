package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.New;
import org.qbicc.type.definition.element.ExecutableElement;

public final class EscapeAnalysisState {
    private static final AttachmentKey<EscapeAnalysisState> KEY = new AttachmentKey<>();
    private final Map<ExecutableElement, List<Call>> callGraph = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, ConnectionGraph> connectionGraphs = new ConcurrentHashMap<>();

    ConnectionGraph getConnectionGraph(ExecutableElement element) {
        return connectionGraphs.get(element);
    }

    // TODO Collection<MethodElement> instead?
    Collection<ExecutableElement> getMethodsVisited() {
        return connectionGraphs.keySet();
    }

    /**
     * Returns the list of callees called from the given method.
     * If method not found in the call graph, an empty list is returned.
     */
    List<Call> getCallees(ExecutableElement element) {
        final List<Call> callees = callGraph.get(element);
        return callees != null ? callees : Collections.emptyList();
    }

    void trackMethod(ExecutableElement element, ConnectionGraph connectionGraph) {
        connectionGraphs.put(element, connectionGraph);
    }

    void trackCall(ExecutableElement from, Call to) {
        callGraph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    boolean isNotEscapingMethod(New new_, ExecutableElement element) {
        final ConnectionGraph connectionGraph = connectionGraphs.get(element);
        return connectionGraph != null && connectionGraph.getEscapeValue(new_).isNoEscape();
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
}
