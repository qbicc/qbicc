package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.New;
import org.qbicc.type.definition.element.ExecutableElement;

final class EscapeAnalysisState {
    private static final AttachmentKey<EscapeAnalysisState> KEY = new AttachmentKey<>();
    private final Map<ExecutableElement, List<Call>> callGraph = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, ConnectionGraph> connectionGraphs = new HashMap<>();

    void trackMethod(ExecutableElement element, ConnectionGraph connectionGraph) {
        connectionGraphs.put(element, connectionGraph);
    }

    boolean isNotEscapingMethod(New new_, ExecutableElement element) {
        final ConnectionGraph connectionGraph = connectionGraphs.get(element);
        return connectionGraph != null && connectionGraph.isNoEscape(new_);
    }

    Stream<ExecutableElement> methods() {
        return connectionGraphs.keySet().stream();
    }

    ConnectionGraph connectionGraph(ExecutableElement element) {
        return connectionGraphs.get(element);
    }

    List<Call> callees(ExecutableElement caller) {
        return callGraph.get(caller);
    }

    void trackCall(ExecutableElement from, Call to) {
        callGraph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
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
