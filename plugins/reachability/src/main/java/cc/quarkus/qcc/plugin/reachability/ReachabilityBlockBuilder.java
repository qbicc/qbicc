package cc.quarkus.qcc.plugin.reachability;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import org.jboss.logging.Logger;

/**
 * A block builder stage which recursively enqueues all referenced executable elements.
 * We implement an RTA-style analysis to identify reachable virtual methods based on
 * the set of reachable call sites and instantiated types.
 */
public class ReachabilityBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger rtaLog = Logger.getLogger("cc.quarkus.qcc.plugin.reachability.rta");

    private final CompilationContext ctxt;

    public ReachabilityBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        return super.invokeStatic(target, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        if (!ctxt.wasEnqueued(target)) {
            processNewTarget(target);
        }
        ctxt.enqueue(target);
        return super.invokeInstance(kind, instance, target, arguments);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        return super.invokeValueStatic(target, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        if (!ctxt.wasEnqueued(target)) {
            processNewTarget(target);
        }
        ctxt.enqueue(target);
        return super.invokeValueInstance(kind, instance, target, arguments);
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        processInstantiatedClass(target.getEnclosingType().validate());
        ctxt.enqueue(target);
        return super.invokeConstructor(instance, target, arguments);
    }

    private void processInstantiatedClass(final ValidatedTypeDefinition type) {
        RTAInfo info = RTAInfo.get(ctxt);
        if (!info.isLiveClass(type)) {
            if (type.hasSuperClass()) {
                processInstantiatedClass(type.getSuperClass());
                info.addLiveClass(type);

                // For every defined virtual method, check to see if it is overriding an enqueued method.
                // If so, then conservatively enqueue it (if it is not already enqueued).
                for (int i = 0; i < type.getMethodCount(); i++) {
                    MethodElement defMethod = type.getMethod(i);
                    if (defMethod.isVirtual() && !ctxt.wasEnqueued(defMethod)) {
                        MethodElement overiddenMethod = type.getSuperClass().resolveMethodElementVirtual(defMethod.getName(), defMethod.getDescriptor());
                        if (overiddenMethod != null && ctxt.wasEnqueued(overiddenMethod)) {
                            ctxt.enqueue(defMethod);
                            rtaLog.debugf("Enqueued method (defined in added type): %s", defMethod);
                        }
                    }
                }
            }
        }
    }

    private void processNewTarget(final MethodElement target) {
        rtaLog.debugf("Adding method (invoke target): %s", target);
        // Traverse the instantiated subclasses of target's defining class and
        // ensure that all overriding implementations of this method are marked invokable.
        ValidatedTypeDefinition definingClass = target.getEnclosingType().validate();
        RTAInfo info = RTAInfo.get(ctxt);
        info.visitLiveSubclasses(target.getEnclosingType().validate(), (sc) -> {
            MethodElement cand = sc.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
            if (!ctxt.wasEnqueued(cand)) {
                rtaLog.debugf("Adding method (subclass override): %s", cand);
                ctxt.enqueue(cand);
            }
        });
    }
}
