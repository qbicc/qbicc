package org.qbicc.plugin.reachability;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.StaticMethodElement;

/**
 * An implementation of Rapid Type Analysis (RTA).
 *
 * RTA augments the basic type reachability and dispatchability tracking
 * done by ReachabilityAnalysis with an additional level of filtering
 * that prevents dispatchable methods that do not have an instantiated
 * receiver type from being invokable.  RTA does this filtering using a
 * single set of instantiated classes for the entire program.
 * In other words, once a New becomes reachable that type is assumed to
 * reach all call sites of dispatchable methods that are defined on that type.
 *
 * The RTA implementation tracks two additional sets of information:
 *   a. classes that have been instantiated
 *   b. deferred instance methods that are dispatchable/reachable but not yet invokable because
 *      no instance of a receiver for that method has been instantiated.
 *
 *  When a new type is instantiated, the algorithm inspects all deferred methods and
 *  moves them to be invokable as necessary.
 *
 *  When a new dispatchable method is discovered, the algorithm inspects all instantiated
 *  classes and either makes the method invokable or deferred.
 */
public final class RapidTypeAnalysis implements ReachabilityAnalysis {
    private final ReachabilityInfo info;
    private final CompilationContext ctxt;

    private final BuildtimeHeapAnalyzer heapAnalyzer;

    // Tracks actually instantiated classes
    private final Set<LoadedTypeDefinition> instantiatedClasses = ConcurrentHashMap.newKeySet();

    // Set of dispatchable, but not yet invokable, instance methods
    private final Set<MethodElement> deferredDispatchableMethods = ConcurrentHashMap.newKeySet();

    // Set of invoked, but not yet invokable, instance methods
    private final Set<MethodElement> deferredExactMethods = ConcurrentHashMap.newKeySet();

    RapidTypeAnalysis(ReachabilityInfo info, CompilationContext ctxt) {
        this.info = info;
        this.ctxt = ctxt;
        heapAnalyzer = new BuildtimeHeapAnalyzer(ctxt);
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
        processInstantiatedClass(ltd, true, currentElement);
    }

    public synchronized void processReachableObject(VmObject object, ExecutableElement currentElement) {
        heapAnalyzer.traceHeap(this, object, currentElement);
    }

    public synchronized void processReachableRuntimeInitializer(final InitializerElement target, ExecutableElement currentElement) {
        if (!ctxt.wasEnqueued(target)) {
            ReachabilityInfo.LOGGER.debugf("Adding <rtinit> %s (potentially invoked from %s)", target, currentElement);
            ctxt.enqueue(target);
        }
    }

    public synchronized void processReachableExactInvocation(final InvokableElement target, ExecutableElement currentElement) {
        if (target instanceof StaticMethodElement me) {
            Facts.get(ctxt).discover(me, ExecutableReachabilityFacts.IS_INVOKED);
        } else if (target instanceof InstanceMethodElement me) {
            Facts.get(ctxt).discover(me, InstanceMethodReachabilityFacts.IS_PROVISIONALLY_INVOKED);
        } else if (target instanceof ConstructorElement ce) {
            Facts.get(ctxt).discover(ce, ExecutableReachabilityFacts.IS_INVOKED);
        }
        if (!ctxt.wasEnqueued(target)) {
            processReachableType(target.getEnclosingType().load(), currentElement);

            if (target instanceof MethodElement me && !me.isStatic()) {
                if (deferredExactMethods.contains(me)) return;
                LoadedTypeDefinition definingClass = me.getEnclosingType().load();
                if (!definingClass.isInterface() && !hasInstantiatedSubclass(definingClass)) {
                    deferredExactMethods.add(me);
                    ReachabilityInfo.LOGGER.debugf("Deferring method %s (invoked exactly in %s, but no instantiated receiver)", target, currentElement);
                    return;
                } else {
                    info.addInvokableInstanceMethod(me);
                }
            }

            ReachabilityInfo.LOGGER.debugf("Adding %s %s (invoked exactly in %s)", target instanceof ConstructorElement ? "<init>" : "method", target, currentElement);
            ctxt.enqueue(target);
        }
    }

    public synchronized void processReachableDispatchedInvocation(final MethodElement target, ExecutableElement currentElement) {
        info.addDispatchableMethod(target);
        if (!info.isInvokableInstanceMethod(target) && !deferredDispatchableMethods.contains(target)) {
            if (hasInstantiatedReceiver(target)) {
                ReachabilityInfo.LOGGER.debugf("Adding dispatched method %s (invoked in %s)", target, currentElement);
                info.addInvokableInstanceMethod(target);
                ctxt.enqueue(target);
            } else {
                ReachabilityInfo.LOGGER.debugf("Deferring method %s (dispatched to in %s, but no instantiated receiver)", target, currentElement);
                deferredDispatchableMethods.add(target);
            }
        }
    }

    public synchronized void processReachableStaticFieldAccess(final StaticFieldElement field, ExecutableElement currentElement) {
        if (!info.isAccessedStaticField(field)) {
            processReachableType(field.getEnclosingType().load(), null);
            info.addAccessedStaticField(field);
            heapAnalyzer.traceHeap(this, field, currentElement);
        }
    }

    public synchronized void processReachableType(final LoadedTypeDefinition ltd, ExecutableElement currentElement) {
        info.addReachableType(ltd);
    }

    public synchronized void processInstantiatedClass(final LoadedTypeDefinition type, boolean onHeapType, ExecutableElement currentElement) {
        if (instantiatedClasses.contains(type)) return;

        if (onHeapType) {
            ReachabilityInfo.LOGGER.debugf("Adding class %s (heap reachable from %s)", type.getDescriptor(), currentElement);
            Facts.get(ctxt).discover(type, TypeReachabilityFacts.IS_ON_HEAP);
        } else {
            ReachabilityInfo.LOGGER.debugf("Adding class %s (instantiated in %s)", type.getDescriptor(), currentElement);
            Facts.get(ctxt).discover(type, TypeReachabilityFacts.IS_INSTANTIATED, TypeReachabilityFacts.IS_ON_HEAP);
        }

        info.addReachableClass(type);
        instantiatedClasses.add(type);

        ArrayList<MethodElement> toRemove = new ArrayList<>();
        for (MethodElement dm : deferredExactMethods ) {
            if (type.isSubtypeOf(dm.getEnclosingType().load())) {
                ReachabilityInfo.LOGGER.debugf("\tDeferred exact method %s is now invokable)", dm);
                toRemove.add(dm);
                info.addInvokableInstanceMethod(dm);
                ctxt.enqueue(dm);
            }
        }
        toRemove.forEach(deferredExactMethods::remove);
        toRemove.clear();

        for (MethodElement dm : deferredDispatchableMethods ) {
            if (type.isSubtypeOf(dm.getEnclosingType().load())) {
                MethodElement cand = type.resolveMethodElementVirtual(type.getContext(), dm.getName(), dm.getDescriptor());
                if (cand != null && cand.equals(dm)) {
                    ReachabilityInfo.LOGGER.debugf("\tDeferred dispatchable method %s is now invokable)", dm);
                    toRemove.add(dm);
                    info.addInvokableInstanceMethod(dm);
                    ctxt.enqueue(dm);
                }
            }
        }
        toRemove.forEach(deferredDispatchableMethods::remove);
    }

    public void clear() {
        instantiatedClasses.clear();
        deferredDispatchableMethods.clear();
        deferredExactMethods.clear();
        heapAnalyzer.clear();
    }

    public void reportStats() {
        ReachabilityInfo.LOGGER.debugf("  Instantiated classes:          %s", instantiatedClasses.size());
        ReachabilityInfo.LOGGER.debugf("  Deferred dispatchable methods: %s", deferredDispatchableMethods.size());
        ReachabilityInfo.LOGGER.debugf("  Deferred exact methods:        %s", deferredExactMethods.size());
    }

    /*
     * RTA Helper methods.
     */
    private boolean hasInstantiatedSubclass(LoadedTypeDefinition ltd) {
        if (instantiatedClasses.contains(ltd)) return true;
        boolean[] ans = { false };
        info.visitReachableSubclassesPreOrder(ltd, sc -> {
            if (instantiatedClasses.contains(sc)) {
                ans[0] = true;
            }
        });
        return ans[0];
    }

    private boolean hasInstantiatedReceiver(MethodElement target) {
        LoadedTypeDefinition definingType = target.getEnclosingType().load();
        if (instantiatedClasses.contains(definingType)) return true;
        boolean[] ans = { false };
        info.visitReachableSubclassesPreOrder(definingType, sc -> {
            if (instantiatedClasses.contains(sc)) {
                MethodElement cand = sc.resolveMethodElementVirtual(definingType.getContext(), target.getName(), target.getDescriptor());
                if (cand != null && cand.equals(target)) {
                    ans[0] = true;
                }
            }
        });
        return ans[0];
    }
}
