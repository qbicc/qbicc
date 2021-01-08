package cc.quarkus.qcc.plugin.reachability;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A block builder stage which recursively enqueues all referenced executable elements.
 * We implement an RTA-style analysis to identify reachable virtual methods based on
 * the set of reachable call sites and instantiated types.
 */
public class ReachabilityBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    private final Set<ValidatedTypeDefinition> instantiatedClasses = ConcurrentHashMap.newKeySet();
    private final Set<MethodElement> invokableMethods = ConcurrentHashMap.newKeySet();
    private final Map<ValidatedTypeDefinition, List<ValidatedTypeDefinition>> subclassMap = new ConcurrentHashMap<ValidatedTypeDefinition, List<ValidatedTypeDefinition>>();

    public ReachabilityBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        return super.invokeStatic(target, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        processInvokedInstanceMethod(target);
        return super.invokeInstance(kind, instance, target, arguments);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        return super.invokeValueStatic(target, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        processInvokedInstanceMethod(target);
        return super.invokeValueInstance(kind, instance, target, arguments);
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        ctxt.enqueue(target);
        processInstantiatedClass(target.getEnclosingType().validate());
        return super.invokeConstructor(instance, target, arguments);
    }

    private void processInstantiatedClass(final ValidatedTypeDefinition type) {
        if (instantiatedClasses.add(type)) {
            if (type.hasSuperClass()) {
                ValidatedTypeDefinition superClass = type.getSuperClass();
                processInstantiatedClass(superClass);
                List<ValidatedTypeDefinition> subclasses = subclassMap.getOrDefault(superClass, new ArrayList<ValidatedTypeDefinition>());
                subclasses.add(type);
            }

            ctxt.info("AJA: Added "+type.getClassType().toFriendlyString());
        }
    }

    private void processInvokedInstanceMethod(final MethodElement target) {
        if (invokableMethods.add(target)) {
            propagateDown(target.getEnclosingType())
            ctxt.info("AJA: Added "+target.getEnclosingType().getDescriptor().getClassName()+"::"+target.getName());
        }
    }
}
