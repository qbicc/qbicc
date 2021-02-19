package cc.quarkus.qcc.plugin.reachability;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
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
            rtaLog.debugf("Adding method (directly invoked): %s", target);
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
            rtaLog.debugf("Adding method (directly invoked): %s", target);
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

    @Override
    public ValueHandle staticField(FieldElement field) {
        DefinedTypeDefinition enclosingType = field.getEnclosingType();
        // initialize referenced field
        ctxt.enqueue(enclosingType.validate().getInitializer());
        return super.staticField(field);
    }

    private void processInstantiatedClass(final ValidatedTypeDefinition type) {
        RTAInfo info = RTAInfo.get(ctxt);
        if (!info.isLiveClass(type)) {
            rtaLog.debugf("Adding reachable class: %s", type.getDescriptor().getClassName());
            if (type.hasSuperClass()) {
                processInstantiatedClass(type.getSuperClass());
                info.addLiveClass(type);

                // For every defined virtual method, check to see if it is overriding an enqueued method.
                // If so, then conservatively enqueue it (if it is not already enqueued).
                for (int i = 0; i < type.getMethodCount(); i++) {
                    MethodElement defMethod = type.getMethod(i);
                    if (!defMethod.isStatic() && !ctxt.wasEnqueued(defMethod)) {
                        MethodElement overiddenMethod = type.getSuperClass().resolveMethodElementVirtual(defMethod.getName(), defMethod.getDescriptor());
                        if (overiddenMethod != null && ctxt.wasEnqueued(overiddenMethod)) {
                            ctxt.enqueue(defMethod);
                            rtaLog.debugf("Newly reachable class: enqueued overriding method: %s", defMethod);
                        }
                    }
                }
            }

            // For every defined virtual method that is not already enqueued,
            // check to see if it is implementing an enqueued interface method.
            // If so, then conservatively enqueue it.
            for (int i = 0; i < type.getMethodCount(); i++) {
                MethodElement defMethod = type.getMethod(i);
                if (!defMethod.isStatic() && !ctxt.wasEnqueued(defMethod)) {
                    for (ValidatedTypeDefinition si : type.getInterfaces()) {
                        MethodElement implementedMethod = si.resolveMethodElementInterface(defMethod.getName(), defMethod.getDescriptor());
                        if (implementedMethod != null && ctxt.wasEnqueued(implementedMethod)) {
                            ctxt.enqueue(defMethod);
                            rtaLog.debugf("Newly reachable class: enqueued implementing method:  %s", defMethod);
                        }
                    }
                }
            }
        }
    }

    private void processNewTarget(final MethodElement target) {
        RTAInfo info = RTAInfo.get(ctxt);
        ValidatedTypeDefinition definingClass = target.getEnclosingType().validate();

        if (definingClass.isInterface()) {
            // First, traverse the instantiated implementors and handle as-if we just saw
            // an invokevirtual of their implementing method.
            info.visitLiveImplementors(definingClass, (c) -> {
                MethodElement cand = c.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                if (!ctxt.wasEnqueued(cand)) {
                    rtaLog.debugf("Adding method (implements): %s", cand);
                    ctxt.enqueue(cand);
                    processNewTarget(cand);
                }
            });

            // Second, traverse the direct superinterfaces and if target is "overriding" a superinterface method
            // handle as-if we just saw an invoke of the superinterface method.
            for (ValidatedTypeDefinition si: definingClass.getInterfaces()) {
                MethodElement cand = si.resolveMethodElementInterface(target.getName(), target.getDescriptor());
                if (cand != null && !ctxt.wasEnqueued(cand)) {
                    rtaLog.debugf("Adding method (subinterface overrides): %s", cand);
                    ctxt.enqueue(cand);
                    processNewTarget(cand);
                }
            }

            // Third: we also need to traverse any extending interfaces and handle as-if we just
            // saw an invokeinterface of thier overridding "implementation" of target.
            // TODO: Need to track the extends relationship in RTAInfo so we can do this computation!
        } else {
            // Traverse the instantiated subclasses of target's defining class and
            // ensure that all overriding implementations of this method are marked invokable.
            info.visitLiveSubclassesPreOrder(definingClass, (sc) -> {
                MethodElement cand = sc.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                if (!ctxt.wasEnqueued(cand)) {
                    rtaLog.debugf("Adding method (subclass overrides): %s", cand);
                    ctxt.enqueue(cand);
                }
            });
        }
    }
}
