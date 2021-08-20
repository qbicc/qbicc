package org.qbicc.plugin.opt.ea;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.Executable;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Node;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class EscapeAnalysisInterMethodHook implements Consumer<CompilationContext> {

    EscapeAnalysisGlobalState globalEscapeAnalysis;

    @Override
    public void accept(CompilationContext ctxt) {
        // TODO run it in parallel?
        globalEscapeAnalysis = EscapeAnalysisGlobalState.get(ctxt);
        globalEscapeAnalysis.methods().forEach(element -> updateConnectionGraphIfNotVisited(element, new HashSet<>()));
    }

    /**
     * Traversal reverse topological order over the program control flow.
     * A bottom-up traversal in which the connection graph of a callee
     * is used to update the connection graph of the caller.
     */
    private EscapeAnalysisMethodState updateConnectionGraphIfNotVisited(ExecutableElement caller, Set<ExecutableElement> visited) {
        if (!visited.add(caller)) {
            return globalEscapeAnalysis.methodState(caller);
        }

        return updateConnectionGraph(caller, visited);
    }

    private EscapeAnalysisMethodState updateConnectionGraph(ExecutableElement caller, Set<ExecutableElement> visited) {
        final EscapeAnalysisMethodState callerState = globalEscapeAnalysis.methodState(caller);
        final List<Call> callees = globalEscapeAnalysis.callees(caller);
        if (callees == null) {
            return callerState;
        }

        for (Call callee : callees) {
            final ExecutableElement calleeElement = ((Executable) callee.getValueHandle()).getExecutable();
            final EscapeAnalysisMethodState calleeState = updateConnectionGraphIfNotVisited(calleeElement, visited);
            if (calleeState != null) {
                updateCallerNodes(callerState, callee, calleeState);
            }
        }

        return callerState;
    }

    private static void updateCallerNodes(EscapeAnalysisMethodState callerState, Call callee, EscapeAnalysisMethodState calleeState) {
        final List<Value> arguments = callee.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            final Value outsideArg = arguments.get(i);
            final ParameterValue insideArg = calleeState.getParameter(i);

            final Collection<Node> mapsToNode = new ArrayList<>();
            mapsToNode.add(outsideArg);

            updateNodes(insideArg, mapsToNode, calleeState, callerState);
        }

        // TODO additionally match callee return value with return value object in the caller?
    }

    private static void updateNodes(Node calleeNode, Collection<Node> mapsToNode, EscapeAnalysisMethodState calleeState, EscapeAnalysisMethodState callerState) {
        for (Value calleePointed : calleeState.getPointsTo(calleeNode)) {
            for (Node callerNode : mapsToNode) {
                final Collection<Value> allCallerPointed = callerState.getPointsTo(callerNode);
                if (allCallerPointed.isEmpty()) {
                    // TODO do I need to insert a new node as the target for callerNode?
                }

                for (Value callerPointed : allCallerPointed) {
                    if (mapsToNode.add(calleePointed)) {
                        // The escape state of caller nodes is marked GlobalEscape,
                        // if the escape state of the callee node is GlobalEscape.
                        callerState.mergeEscapeValue(callerPointed, calleeState, calleePointed);

                        for (InstanceFieldOf calleeField : calleeState.getFieldEdges(calleePointed)) {
                            final String calleeFieldName = calleeField.getVariableElement().getName();
                            final Collection<Node> callerField = callerState.getFieldEdges(callerPointed).stream()
                                .filter(field -> Objects.equals(field.getVariableElement().getName(), calleeFieldName))
                                .collect(Collectors.toList());

                            updateNodes(calleeField, callerField, calleeState, callerState);
                        }
                    }
                }
            }
        }
    }
}
