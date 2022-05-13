package org.qbicc.plugin.opt.ea;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.Executable;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NamedElement;

public class EscapeAnalysisInterMethodAnalysis implements Consumer<CompilationContext> {

    @Override
    public void accept(CompilationContext ctxt) {
        new ConnectionGraphUpdater(ctxt).run();
    }

    private static final class ConnectionGraphUpdater implements Runnable {
        final Set<ExecutableElement> visited = new HashSet<>();
        final EscapeAnalysisState state;
        final ReachabilityInfo rta;

        private ConnectionGraphUpdater(CompilationContext ctxt) {
            this.state = EscapeAnalysisState.get(ctxt);
            this.rta = ReachabilityInfo.get(ctxt);
        }

        @Override
        public void run() {
            state.getMethodsVisited().forEach(this::updateConnectionGraphIfNotVisited);
        }

        /**
         * Traversal reverse topological order over the program control flow.
         * A bottom-up traversal in which the connection graph of a callee
         * is used to update the connection graph of the caller.
         */
        private ConnectionGraph updateConnectionGraphIfNotVisited(ExecutableElement caller) {
            if (!visited.add(caller)) {
                return state.getConnectionGraph(caller);
            }

            return updateConnectionGraph(caller);
        }

        private ConnectionGraph updateConnectionGraph(ExecutableElement caller) {
            final ConnectionGraph callerCG = findConnectionGraph(caller);
            if (callerCG == null) {
                return null;
            }

            // 4.1 Update Connection Graph at Method Entry
            // Skipped because arguments are initialized as argument escape during intra method analysis phase

            for (Call callee : state.getCallees(caller)) {
                final ExecutableElement calleeElement = ((Executable) callee.getValueHandle()).getExecutable();
                final ConnectionGraph calleeCG = updateConnectionGraphIfNotVisited(calleeElement);
                if (calleeCG != null) {
                    // 4.4 Update Connection Graph Immediately After a Method Invocation
                    callerCG.updateAfterInvokingMethod(callee, calleeCG);
                }
            }

            // 4.2 Update Connection Graph at Method Exit
            callerCG.updateAtMethodExit();

            return callerCG;
        }

        /**
         * Find a connection graph for a given executable element.
         */
        private ConnectionGraph findConnectionGraph(ExecutableElement executable) {
            ConnectionGraph cg = state.getConnectionGraph(executable);
            // For direct, concrete, method invocations, the escape analysis state will contain a connection graph for it.
            if (cg != null) {
                // However, we need to take into account method overrides
                final Set<ExecutableElement> subclasses = findSubclasses(executable, executable.getEnclosingType().load());
                if (subclasses.isEmpty()) {
                    return cg;
                }

                // If we find any method overrides, union them with the original method connection graph
                return unionConnectionGraph(subclasses, cg);
            }

            // For interface methods, find connection graphs for all methods that might be a target of a call, and union them.
            cg = findInterfaceConnectionGraph(executable);
            if (cg != null) {
                return cg;
            }

            // For abstract methods, find connection graphs for all available implementations
            cg = findAbstractConnectionGraph(executable);
            if (cg != null) {
                return cg;
            }

            return cg;
        }

        private Set<ExecutableElement> findSubclasses(ExecutableElement executable, LoadedTypeDefinition loadedExecutableType) {
            final Set<ExecutableElement> implementors = new HashSet<>();
            rta.visitReachableSubclassesPostOrder(loadedExecutableType, type -> findReachableMethods(executable, type, implementors));
            return implementors;
        }

        private ConnectionGraph findInterfaceConnectionGraph(ExecutableElement executable) {
            final DefinedTypeDefinition enclosingType = executable.getEnclosingType();
            if (!enclosingType.isInterface()) {
                return null;
            }

            final LoadedTypeDefinition loadedExecutableType = enclosingType.load();
            final Set<ExecutableElement> implementors = findInterfaceImplementors(executable, loadedExecutableType);
            if (implementors.isEmpty()) {
                return null;
            }

            // Get connection graphs for these implementors and union them.
            // The implementors might be calling other methods (e.g. generic interface bridges),
            // so make sure they're connection graphs have been updated before going and making a union.
            return unionConnectionGraph(implementors, new ConnectionGraph(executable));
        }

        private ConnectionGraph findAbstractConnectionGraph(ExecutableElement executable) {
            boolean isAbstractMethod = executable instanceof MethodElement && ((MethodElement) executable).isAbstract();
            if (!isAbstractMethod) {
                return null;
            }

            // Find the interface in which the executable was defined
            final LoadedTypeDefinition loadedExecutableType = executable.getEnclosingType().load();

            // Find all implementors that contain method definitions
            final Set<ExecutableElement> subclasses = findSubclasses(executable, loadedExecutableType);

            // If there are no implementors, skip
            if (subclasses.isEmpty()) {
                return null;
            }

            // Get connection graphs for these implementors and union them.
            // The implementors might be calling other methods (e.g. generic interface bridges),
            // so make sure they're connection graphs have been updated before going and making a union.
            return unionConnectionGraph(subclasses, new ConnectionGraph(executable));
        }

        private ConnectionGraph unionConnectionGraph(Set<ExecutableElement> methods, ConnectionGraph initValue) {
            return methods.stream()
                .map(this::updateConnectionGraphIfNotVisited)
                .reduce(initValue, ConnectionGraph::union);
        }

        private Set<ExecutableElement> findInterfaceImplementors(ExecutableElement executable, LoadedTypeDefinition loadedExecutableType) {
            final Set<ExecutableElement> implementors = new HashSet<>();
            // TODO create a type to carry implementors and executable (and add logging when implementors added to)
            rta.visitReachableImplementors(loadedExecutableType, type -> findReachableMethods(executable, type, implementors));
            return implementors;
        }

        private void findReachableMethods(ExecutableElement executable, LoadedTypeDefinition type, Set<ExecutableElement> implementors) {
            final MethodElement[] instanceMethods = type.getInstanceMethods();
            for (MethodElement instanceMethod : instanceMethods) {
                if (isImplementationOf(executable, instanceMethod) && isReachable(instanceMethod)) {
                    implementors.add(instanceMethod);
                }
            }
        }

        // TODO this does not seem to be really working (that's why there's the workaround in CG.updateAfterInvokingMethod)
        private boolean isReachable(ExecutableElement executable) {
            return executable instanceof MethodElement && rta.isDispatchableMethod((MethodElement) executable);
        }
    }

    private static boolean isImplementationOf(ExecutableElement executable, MethodElement instanceMethod) {
        if (executable instanceof NamedElement) {
            final String executableName = ((NamedElement) executable).getName();
            return executableName.equals(instanceMethod.getName())
                && executable.getDescriptor().equals(instanceMethod.getDescriptor());
        }

        return false;
    }
}
