package org.qbicc.plugin.opt.ea;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.Executable;
import org.qbicc.type.definition.element.ExecutableElement;

public class EscapeAnalysisInterMethodAnalysis implements Consumer<CompilationContext> {
    final Set<ExecutableElement> visited = new HashSet<>();

    @Override
    public void accept(CompilationContext ctxt) {
        EscapeAnalysisState state = EscapeAnalysisState.get(ctxt);
        state.getMethodsVisited().forEach(element -> updateConnectionGraphIfNotVisited(element, state));
    }

    /**
     * Traversal reverse topological order over the program control flow.
     * A bottom-up traversal in which the connection graph of a callee
     * is used to update the connection graph of the caller.
     */
    private ConnectionGraph updateConnectionGraphIfNotVisited(ExecutableElement caller, EscapeAnalysisState state) {
        if (!visited.add(caller)) {
            return state.getConnectionGraph(caller);
        }

        return updateConnectionGraph(caller, state);
    }

    private ConnectionGraph updateConnectionGraph(ExecutableElement caller, EscapeAnalysisState state) {
        final ConnectionGraph callerCG = state.getConnectionGraph(caller);
        if (callerCG == null) {
            return null;
        }

        // 4.1 Update Connection Graph at Method Entry
        callerCG.updateAtMethodEntry();

        for (Call callee : state.getCallees(caller)) {
            final ExecutableElement calleeElement = ((Executable) callee.getValueHandle()).getExecutable();
            final ConnectionGraph calleeCG = updateConnectionGraphIfNotVisited(calleeElement, state);
            if (calleeCG != null) {
                // 4.4 Update Connection Graph Immediately After a Method Invocation
                callerCG.updateAfterInvokingMethod(callee, calleeCG);
            }
        }

        // 4.2 Update Connection Graph at Method Exit
        callerCG.updateAtMethodExit();

        return callerCG;
    }
}
