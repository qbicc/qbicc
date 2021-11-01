package org.qbicc.plugin.reachability;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
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

    // Set of reachable, but not yet invokable, instance methods
    private final Set<MethodElement> deferredInstanceMethods = ConcurrentHashMap.newKeySet();

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

    public synchronized void processBuildtimeInstantiatedObjectType(LoadedTypeDefinition ltd, LoadedTypeDefinition staticRootType) {
        processInstantiatedClass(ltd, true, true, staticRootType.getInitializer());
        processClassInitialization(ltd);
    }

    public synchronized void processReachableStaticInvoke(final InvokableElement target, ExecutableElement originalElement) {
        if (!ctxt.wasEnqueued(target)) {
            ReachabilityInfo.LOGGER.debugf("Adding method %s (statically invoked in %s)", target, originalElement);
            ctxt.enqueue(target);
        }
    }

    public synchronized void processReachableConstructorInvoke(LoadedTypeDefinition ltd, ConstructorElement target, ExecutableElement originalElement) {
        processInstantiatedClass(ltd, true, false, originalElement);
        processClassInitialization(ltd);
        if (!ctxt.wasEnqueued(target)) {
            ReachabilityInfo.LOGGER.debugf("Adding <init> %s (invoked in %s)", target, originalElement);
            ctxt.enqueue(target);
        }
    }

    public synchronized void processReachableInstanceMethodInvoke(final MethodElement target, ExecutableElement originalElement) {
        if (!info.isInvokableMethod(target)) {
            LoadedTypeDefinition definingClass = target.getEnclosingType().load();
            if (definingClass.isInterface() || info.isInstantiatedClass(definingClass)) {
                ReachabilityInfo.LOGGER.debugf("Adding method %s (directly invoked in %s)", target, originalElement);
                info.addInvokableMethod(target);
                ctxt.enqueue(target);
                if (!target.isPrivate()) {
                    propagateInvokabilityToOverrides(target);
                }
            } else {
                ReachabilityInfo.LOGGER.debugf("Deferring method %s (invoked in %s, but no instantiated receiver)", target, originalElement);
                info.addReachableClass(definingClass);
                deferredInstanceMethods.add(target);
            }
        }
    }

    public synchronized void processStaticElementInitialization(final LoadedTypeDefinition ltd, BasicElement cause, ExecutableElement originalElement) {
        if (info.isInitializedType(ltd)) return;
        ReachabilityInfo.LOGGER.debugf("Initializing %s (static access to %s in %s)", ltd.getInternalName(), cause, originalElement);
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

        heapAnalyzer.traceHeap(ctxt, this, ltd);

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

    public synchronized void processInstantiatedClass(final LoadedTypeDefinition type, boolean directlyInstantiated, boolean onHeapType, ExecutableElement originalElement) {
        if (info.isInstantiatedClass(type)) return;

        if (onHeapType) {
            ReachabilityInfo.LOGGER.debugf("Adding class %s (heap reachable from static of %s)", type.getDescriptor().getClassName(), originalElement.getEnclosingType().getDescriptor().getClassName());
        } else if (directlyInstantiated) {
            ReachabilityInfo.LOGGER.debugf("Adding class %s (instantiated in %s)", type.getDescriptor().getClassName(), originalElement);
        } else {
            ReachabilityInfo.LOGGER.debugf("\tadding ancestor class: %s", type.getDescriptor().getClassName());
        }
        info.addReachableClass(type);
        info.addInstantiatedClass(type);

        // It's critical that we recur to handle our superclass first.  That means all of its invokable/deferred methods
        // that we override will be processed before we process our own defined instance methods below.
        if (type.hasSuperClass()) {
            processInstantiatedClass(type.getSuperClass(), false, false, originalElement);
        }

        // TODO: Now that we are explicitly tracking directly invoked deferred methods, we might be able to
        //       replace the checks below for overriding an invokable method with logic that instead
        //       adds overridden invoked methods to the deferred set in addReachableClass and processInvokableInstanceMethod
        //       It's not clear yet which of these options is simpler/more efficient/more maintainable...

        // For every instance method that is not already invokable,
        // check to see if it is either (a) a deferred invoked method or (b) overriding an invokable method and thus should be enqueued.
        for (MethodElement im : type.getInstanceMethods()) {
            if (!info.isInvokableMethod(im)) {
                if (isDeferredInstanceMethod(im)) {
                    ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: enqueued deferred instance method: %s", im);
                    deferredInstanceMethods.remove(im);
                    info.addInvokableMethod(im);
                    ctxt.enqueue(im);
                } else if (type.hasSuperClass()) {
                    MethodElement overiddenMethod = type.getSuperClass().resolveMethodElementVirtual(im.getName(), im.getDescriptor());
                    if (overiddenMethod != null && info.isInvokableMethod(overiddenMethod)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: enqueued overriding instance method: %s", im);
                        info.addInvokableMethod(im);
                        ctxt.enqueue(im);
                    }
                }
            }
        }

        // For every invokable interface method, make sure my implementation of that method is invokable.
        for (LoadedTypeDefinition i : type.getInterfaces()) {
            for (MethodElement sig : i.getInstanceMethods()) {
                if (info.isInvokableMethod(sig)) {
                    MethodElement impl = type.resolveMethodElementVirtual(sig.getName(), sig.getDescriptor());
                    if (impl != null && !info.isInvokableMethod(impl)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: enqueued implementing method:  %s", impl);
                        info.addInvokableMethod(impl);
                        deferredInstanceMethods.remove(impl); // might not be deferred, but remove is a no-op if it isn't present
                        ctxt.enqueue(impl);
                    }
                }
            }
        }
    }

    public void clear() {
        deferredInstanceMethods.clear();
        heapAnalyzer.clear();
    }

    public void reportStats() {
        ReachabilityInfo.LOGGER.debugf("  Deferred instance methods:  %s", deferredInstanceMethods.size());
    }

    /*
     * RTA Helper methods.
     */

    boolean isDeferredInstanceMethod(MethodElement meth) {
        return deferredInstanceMethods.contains(meth);
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
                if (cand != null && !info.isInvokableMethod(cand)) {
                    ReachabilityInfo.LOGGER.debugf("\tadding method (implements): %s", cand);
                    info.addInvokableMethod(cand);
                    ctxt.enqueue(cand);
                    propagateInvokabilityToOverrides(cand);
                }
            });
        } else {
            // Traverse the instantiated subclasses of target's defining class and
            // ensure that all overriding implementations of this method are marked invokable.
            info.visitReachableSubclassesPreOrder(definingClass, (sc) -> {
                if (info.isInstantiatedClass(sc)) {
                    MethodElement cand = sc.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                    if (!info.isInvokableMethod(cand)) {
                        ReachabilityInfo.LOGGER.debugf("\tadding method (subclass overrides): %s", cand);
                        info.addInvokableMethod(cand);
                        ctxt.enqueue(cand);
                    }
                }
            });
        }
    }
}
