package org.qbicc.plugin.opt.ea;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.Executable;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class EscapeAnalysisInterMethodHook implements Consumer<CompilationContext> {

    EscapeAnalysisState escapeAnalysisState;

    @Override
    public void accept(CompilationContext ctxt) {
        // TODO run it in parallel?
        escapeAnalysisState = EscapeAnalysisState.get(ctxt);
        final Set<ExecutableElement> visited = new HashSet<>();
        escapeAnalysisState.methods().forEach(element -> updateConnectionGraphIfNotVisited(element, visited));
    }

    /**
     * Traversal reverse topological order over the program control flow.
     * A bottom-up traversal in which the connection graph of a callee
     * is used to update the connection graph of the caller.
     */
    private ConnectionGraph updateConnectionGraphIfNotVisited(ExecutableElement caller, Set<ExecutableElement> visited) {
        if (!visited.add(caller)) {
            return escapeAnalysisState.connectionGraph(caller);
        }

        return updateConnectionGraph(caller, visited);
    }

    private ConnectionGraph updateConnectionGraph(ExecutableElement caller, Set<ExecutableElement> visited) {
        final ConnectionGraph callerCG = escapeAnalysisState.connectionGraph(caller);
        if (callerCG == null) {
            return null;
        }

        final List<Call> callees = escapeAnalysisState.callees(caller);
        if (callees == null) {
            return callerCG;
        }

        for (Call callee : callees) {
            final ExecutableElement calleeElement = ((Executable) callee.getValueHandle()).getExecutable();
            final ConnectionGraph calleeCG = updateConnectionGraphIfNotVisited(calleeElement, visited);
            if (calleeCG != null) {
                callerCG.update(callee, calleeCG);
            }
        }

        return callerCG;
    }
}
