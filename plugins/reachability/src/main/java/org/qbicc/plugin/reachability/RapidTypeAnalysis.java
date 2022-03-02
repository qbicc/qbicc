package org.qbicc.plugin.reachability;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of Rapid Type Analysis (RTA).
 *
 * Summary of the key ideas of our RTA implementation:
 * 1. As types become reachable, we incrementally add them to the subclass/interface hierarchy maps.
 *    We maintain the invariant that when we make a type reachable its supertype and all of its
 *    implemented interfaces are also reachable and all the edges in the hierarchies are added.
 *    This allows us to use the RTA hierarchy information to build the runtime data structures
 *    we use for dynamic type checking without artifacts from, reachable but not instantiated/initialized types.
 * 2. Similarly, as soon as an invocation of an interface method becomes reachable we consider that
 *    abstract method to be invokable and eagerly propagate the invokablity of that signature up/down the
 *    interface hierarchy. When an interface becomes reachable, we immediately check its superinterfaces
 *    for invokable methods and make its "overriding" methods invokable.
 * 3. Instance methods of a class that have been invoked may become invokable immediately if the
 *    class has already been instantiated.  If the class is only reachable, but not instantiated
 *    then these methods are added to a set of deferred instance methods.  If their defining class
 *    later is instantiated, these deferred methods are make invokable.
 * 4. We also handle class initialization semantics, to be able to determine which <clinit>
 *     methods become invokable.
 */
public final class RapidTypeAnalysis implements ReachabilityAnalysis {
    private final ReachabilityInfo info;
    private final CompilationContext ctxt;

    private final BuildtimeHeapAnalyzer heapAnalyzer = new BuildtimeHeapAnalyzer();

    // Set of dispatchable, but not yet invokable, instance methods
    private final Set<MethodElement> deferredDispatchableMethods = ConcurrentHashMap.newKeySet();

    // Set of invoked, but not yet invokable, instance methods
    private final Set<MethodElement> deferredExactMethods = ConcurrentHashMap.newKeySet();

    RapidTypeAnalysis(ReachabilityInfo info, CompilationContext ctxt) {
        this.info = info;
        this.ctxt = ctxt;
    }

    /*
     * Implementation of the ReachabilityAnalysis interface
     */

    public synchronized void processArrayElementType(ObjectType elemType) {
        if (elemType instanceof ClassObjectType) {
            info.addReachableClass(elemType.getDefinition().load());
        } else if (elemType instanceof InterfaceObjectType) {
            info.addReachableInterface(elemType.getDefinition().load());
        }
    }

    public synchronized void processBuildtimeInstantiatedObjectType(LoadedTypeDefinition ltd, ExecutableElement currentElement) {
        processInstantiatedClass(ltd, true, true, currentElement);
        processClassInitialization(ltd);
    }

    public synchronized void processReachableObjectLiteral(ObjectLiteral objectLiteral, ExecutableElement currentElement) {
        heapAnalyzer.traceHeap(ctxt, this, objectLiteral.getValue(), currentElement);
    }

    public synchronized void processReachableRuntimeInitializer(final InitializerElement target, ExecutableElement currentElement) {
        if (!ctxt.wasEnqueued(target)) {
            ReachabilityInfo.LOGGER.debugf("Adding <rtinit> %s (potentially invoked from %s)", target, currentElement);
            ctxt.enqueue(target);
        }
    }

    public synchronized void processReachableExactInvocation(final InvokableElement target, ExecutableElement currentElement) {
        if (!ctxt.wasEnqueued(target)) {
            if (target instanceof MethodElement me && !me.isStatic()) {
                LoadedTypeDefinition definingClass = me.getEnclosingType().load();
                if (!definingClass.isInterface() && !info.isInstantiatedClass(definingClass)) {
                    info.addReachableClass(definingClass);
                    deferredExactMethods.add(me);
                    ReachabilityInfo.LOGGER.debugf("Deferring method %s (invoked exactly in %s, but no instantiated receiver)", target, currentElement);
                    return;
                }
            }

            ReachabilityInfo.LOGGER.debugf("Adding %s %s (invoked exactly in %s)", target instanceof ConstructorElement ? "<init>" : "method", target, currentElement);
            ctxt.enqueue(target);
        }
    }

    public synchronized void processReachableDispatchedInvocation(final MethodElement target, ExecutableElement currentElement) {
        if (!info.isDispatchableMethod(target)) {
            LoadedTypeDefinition definingClass = target.getEnclosingType().load();
            if (definingClass.isInterface() || info.isInstantiatedClass(definingClass)) {
                if (currentElement == null) {
                    ReachabilityInfo.LOGGER.debugf("\tadding method %s (dispatch mechanism induced)", target);
                } else {
                    ReachabilityInfo.LOGGER.debugf("Adding dispatched method %s (invoked in %s)", target, currentElement);
                }
                info.addDispatchableMethod(target);
                ctxt.enqueue(target);
                if (!target.isPrivate()) {
                    propagateInvokabilityToOverrides(target);
                    if (!definingClass.isInterface() && definingClass.getSuperClass() != null) {
                        // Because we are doing per-class vtables, we also need to ensure that if target overrides a
                        // superclass method that the superclass method is also considered dispatched to to ensure compatible vtable layouts
                        MethodElement superMethod = definingClass.getSuperClass().resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                        if (superMethod != null) {
                            processReachableDispatchedInvocation(superMethod, null);
                        }
                    }
                }
            } else {
                ReachabilityInfo.LOGGER.debugf("Deferring method %s (dispatched to in %s, but no instantiated receiver)", target, currentElement);
                info.addReachableClass(definingClass);
                deferredDispatchableMethods.add(target);
            }
        }
    }

    public synchronized void processReachableStaticFieldAccess(final FieldElement field, ExecutableElement currentElement) {
        if (!info.isAccessedStaticField(field)) {
            info.addAccessedStaticField(field);
            heapAnalyzer.traceHeap(ctxt, this, field, currentElement);
        }
    }

    public synchronized void processStaticElementInitialization(final LoadedTypeDefinition ltd, BasicElement cause, ExecutableElement currentElement) {
        if (info.isInitializedType(ltd)) return;
        ReachabilityInfo.LOGGER.debugf("Initializing %s (static access to %s in %s)", ltd.getInternalName(), cause, currentElement);
        if (ltd.isInterface()) {
            info.addReachableInterface(ltd);
            // JLS: accessing a static field/method of an interface only causes local <clinit> execution
            info.addInitializedType(ltd);
        } else {
            // JLS: accessing a static field/method of a class <clinit> all the way up the class/interface hierarchy
            processClassInitialization(ltd);
        }
    }

    public synchronized void processClassInitialization(final LoadedTypeDefinition ltd) {
        Assert.assertFalse(ltd.isInterface());
        if (info.isInitializedType(ltd)) return;
        info.addReachableClass(ltd);

        if (ltd.hasSuperClass()) {
            // force superclass initialization
            processClassInitialization(ltd.getSuperClass());
        }
        info.addInitializedType(ltd);

        // Annoyingly, because an intermediate interface could be marked initialized due to a static field
        // access which doesn't cause the initialization of its superinterfaces, we can't short-circuit
        // this walk up the entire interface hierarchy when we hit an already initialized interfaces.
        ArrayDeque<LoadedTypeDefinition> worklist = new ArrayDeque<>(List.of(ltd.getInterfaces()));
        while (!worklist.isEmpty()) {
            LoadedTypeDefinition i = worklist.pop();
            if (i.declaresDefaultMethods() && !info.isInitializedType(i)) {
                info.addInitializedType(i);
            }
            worklist.addAll(List.of(i.getInterfaces()));
        }
    }

    public synchronized void processInstantiatedClass(final LoadedTypeDefinition type, boolean directlyInstantiated, boolean onHeapType, ExecutableElement currentElement) {
        if (info.isInstantiatedClass(type)) return;

        if (onHeapType) {
            ReachabilityInfo.LOGGER.debugf("Adding class %s (heap reachable from %s)", type.getDescriptor().getClassName(), currentElement);
        } else if (directlyInstantiated) {
            ReachabilityInfo.LOGGER.debugf("Adding class %s (instantiated in %s)", type.getDescriptor().getClassName(), currentElement);
        } else {
            ReachabilityInfo.LOGGER.debugf("\tadding ancestor class: %s", type.getDescriptor().getClassName());
        }
        info.addReachableClass(type);
        info.addInstantiatedClass(type);

        // It's critical that we recur to handle our superclass first.  That means all of its invokable/deferred methods
        // that we override will be processed before we process our own defined instance methods below.
        if (type.hasSuperClass()) {
            processInstantiatedClass(type.getSuperClass(), false, false, currentElement);
        }

        // TODO: Now that we are explicitly tracking directly invoked deferred methods, we might be able to
        //       replace the checks below for overriding an invokable method with logic that instead
        //       adds overridden invoked methods to the deferred set in addReachableClass and processInvokableInstanceMethod
        //       It's not clear yet which of these options is simpler/more efficient/more maintainable...

        // For every instance method that is not already dispatchable,
        // check to see if it is either (a) a deferred invoked method or (b) overriding an invokable method and thus should be enqueued.
        for (MethodElement im : type.getInstanceMethods()) {
            if (!info.isDispatchableMethod(im)) {
                if (isDeferredDispatchableMethod(im)) {
                    ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: dispatching to deferred instance method: %s", im);
                    deferredDispatchableMethods.remove(im);
                    processReachableDispatchedInvocation(im, null);
                } else if (type.hasSuperClass()) {
                    MethodElement overiddenMethod = type.getSuperClass().resolveMethodElementVirtual(im.getName(), im.getDescriptor());
                    if (overiddenMethod != null && info.isDispatchableMethod(overiddenMethod)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: dispatching to overriding instance method: %s", im);
                        info.addDispatchableMethod(im);
                        ctxt.enqueue(im);
                    }
                }
                if (isDeferredExactMethod(im)) {
                    ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: invoking deferred exact method: %s", im);
                    deferredExactMethods.remove(im);
                    processReachableExactInvocation(im, null);
                }
            }
        }

        // For every invokable interface method, make sure my implementation of that method is invokable.
        for (LoadedTypeDefinition i : type.getInterfaces()) {
            for (MethodElement sig : i.getInstanceMethods()) {
                if (info.isDispatchableMethod(sig)) {
                    MethodElement impl = type.resolveMethodElementVirtual(sig.getName(), sig.getDescriptor());
                    if (impl != null && !info.isDispatchableMethod(impl)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: simulating dispatch to implementing method:  %s", impl);
                        deferredDispatchableMethods.remove(impl); // might not be deferred, but remove is a no-op if it isn't present
                        deferredExactMethods.remove(impl); // might not be deferred, but remove is a no-op if it isn't present
                        processReachableDispatchedInvocation(impl, null);
                    }
                }
            }
        }
    }

    public void clear() {
        deferredDispatchableMethods.clear();
        deferredExactMethods.clear();
        heapAnalyzer.clear();
    }

    public void reportStats() {
        ReachabilityInfo.LOGGER.debugf("  Deferred dispatchable methods: %s", deferredDispatchableMethods.size());
        ReachabilityInfo.LOGGER.debugf("  Deferred exact methods:        %s", deferredExactMethods.size());
    }

    /*
     * RTA Helper methods.
     */

    boolean isDeferredDispatchableMethod(MethodElement meth) {
        return deferredDispatchableMethods.contains(meth);
    }

    boolean isDeferredExactMethod(MethodElement meth) {
        return deferredExactMethods.contains(meth);
    }

    void propagateInvokabilityToOverrides(final MethodElement target) {
        LoadedTypeDefinition definingClass = target.getEnclosingType().load();

        if (definingClass.isInterface()) {
            // Traverse the reachable extenders and implementors and handle as-if we just saw
            // an invokevirtual/invokeinterface  of their overriding/implementing method
            info.visitReachableImplementors(definingClass, (c) -> {
                MethodElement cand = null;
                if (c.isInterface()) {
                    cand = c.resolveMethodElementInterface(target.getName(), target.getDescriptor());
                } else if (info.isInstantiatedClass(c)) {
                    cand = c.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                }
                if (cand != null && !info.isDispatchableMethod(cand)) {
                    ReachabilityInfo.LOGGER.debugf("\tsimulating dispatch to implementing method: %s", cand);
                    processReachableDispatchedInvocation(cand, null);
                }
            });
        } else {
            // Traverse the instantiated subclasses of target's defining class and
            // ensure that all overriding implementations of this method are marked dispatchable.
            info.visitReachableSubclassesPreOrder(definingClass, (sc) -> {
                if (info.isInstantiatedClass(sc)) {
                    MethodElement cand = sc.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                    if (!info.isDispatchableMethod(cand)) {
                        ReachabilityInfo.LOGGER.debugf("\tadding dispatchable method (subclass overrides): %s", cand);
                        info.addDispatchableMethod(cand);
                        ctxt.enqueue(cand);
                    }
                }
            });
        }
    }
}
