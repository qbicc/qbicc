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

final class EscapeAnalysisGlobalState {
    private static final AttachmentKey<EscapeAnalysisGlobalState> KEY = new AttachmentKey<>();
    private final Map<ExecutableElement, List<Call>> callGraph = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, EscapeAnalysisMethodState> escapeAnalysisMethods = new HashMap<>();

    void trackMethod(ExecutableElement element, EscapeAnalysisMethodState methodEscapeAnalysis) {
        escapeAnalysisMethods.put(element, methodEscapeAnalysis);
    }

    boolean isNotEscapingMethod(New new_, ExecutableElement element) {
        final EscapeAnalysisMethodState methodEscapeState = escapeAnalysisMethods.get(element);
        return methodEscapeState != null && methodEscapeState.isNoEscape(new_);
    }

    Stream<ExecutableElement> methods() {
        return escapeAnalysisMethods.keySet().stream();
    }

    EscapeAnalysisMethodState methodState(ExecutableElement element) {
        return escapeAnalysisMethods.get(element);
    }

    List<Call> callees(ExecutableElement caller) {
        return callGraph.get(caller);
    }

    void trackCall(ExecutableElement from, Call to) {
        callGraph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    static EscapeAnalysisGlobalState get(CompilationContext ctxt) {
        EscapeAnalysisGlobalState globalEscapeAnalysis = ctxt.getAttachment(KEY);
        if (globalEscapeAnalysis == null) {
            globalEscapeAnalysis = new EscapeAnalysisGlobalState();
            EscapeAnalysisGlobalState appearing = ctxt.putAttachmentIfAbsent(KEY, globalEscapeAnalysis);
            if (appearing != null) {
                globalEscapeAnalysis = appearing;
            }
        }
        return globalEscapeAnalysis;
    }
}
