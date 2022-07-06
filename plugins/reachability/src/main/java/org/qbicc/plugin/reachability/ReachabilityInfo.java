package org.qbicc.plugin.reachability;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * ReachabilityInfo records the types and methods that have been determined to be reachable
 * and/or instantiatable/invokable from a program entrypoint.
 *
 * A class or interface is reachable if a reference to
 * (1) a program element it defines or (2) its java.lang.Class instance
 * could be executed by an invokable method.
 *
 * If a class is reachable, its superclass and all its implemented interfaces are
 * also considered to be reachable.
 *
 * If an interface is reachable, then all of its superinterfaces are also considered
 * to be reachable.
 *
 * A class is called instantiated if a `new` of the class
 * could be executed by an invokable method.
 *
 * A reachable direct call of a static method or function makes that method invokable.
 *
 * A reachable direct call of an instance method (invokespecial) that has at least one
 * instantiated receiver class makes that method invokable.
 *
 * Instance methods may also be invoked via dynamically dispatched calls (invokevirtual, invokeinterface).
 * We track dispatchable methods and the subset of those dispatchable methods that are actually invokable.
 * Being dispatchable indicates that there is a reachable dispatched call
 * of at least one method in a method family (name+descriptor+override hierarchy).
 * Being invokable indicates that both the method is dispatchable and that
 * there is an instantiated class such that if an instance of that class were
 * used as the receiver of a dispatched call then the method would be invoked dynamically.
 * Therefore, space must be allocated in the runtime method dispatching structures for
 * all dispatchable methods, but we need only enqueue/compile the invokable instance methods.
 * Slots for dispatchable, but not invokable methods will never be used at runtime
 * and can be filled with an error thunk.  The actual tracking of instantiated classes
 * and the resulting computing of invokability is delegated to the ReachabilityAnalysis
 * implementation, which provides an extension point for using different algorithms.
 *
 * All classes and interfaces that are reachable after the ANALYZE phase completes
 * will be assigned typeIds.
 *
 * All methods that are invokable at the end of the ANALYZE phase
 * will be carried through the rest of the phases and compiled.
 */
public class ReachabilityInfo {
    static final Logger LOGGER = Logger.getLogger("org.qbicc.plugin.reachability");
    private static final AttachmentKey<ReachabilityInfo> KEY = new AttachmentKey<>();

    // Tracks reachable classes and their (direct) reachable subclasses
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> classHierarchy = new ConcurrentHashMap<>();
    // Tracks reachable interfaces and their (direct) reachable implementors
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> interfaceHierarchy = new ConcurrentHashMap<>();

    // Set of instance methods of reachable types that are dispatched to (need slots allocated in vtable/itable dispatching tables)
    private final Set<MethodElement> dispatchableMethods = ConcurrentHashMap.newKeySet();
    // Set of instance methods that are both dispatchable and have an instantiated receiver class
    private final Set<MethodElement> invokableInstanceMethods = ConcurrentHashMap.newKeySet();

    // Set of static fields that are potentially accessed by reachable code
    private final Set<FieldElement> accessedStaticField = ConcurrentHashMap.newKeySet();

    private final ReachabilityAnalysis analysis;
    private final CompilationContext ctxt;

    private ReachabilityInfo(final CompilationContext ctxt) {
        // TODO: Currently hardwired to RTA; eventually will support multiple analysis algorithms
        this.analysis = new RapidTypeAnalysis(this, ctxt);
        this.ctxt = ctxt;
    }

    public static ReachabilityInfo get(CompilationContext ctxt) {
        ReachabilityInfo info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new ReachabilityInfo(ctxt);
            ReachabilityInfo appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public static void clear(CompilationContext ctxt) {
        ReachabilityInfo info = get(ctxt);
        info.classHierarchy.clear();
        info.interfaceHierarchy.clear();
        info.dispatchableMethods.clear();
        info.invokableInstanceMethods.clear();
        info.accessedStaticField.clear();
        info.analysis.clear();
    }

    public static void reportStats(CompilationContext ctxt) {
        ReachabilityInfo info = get(ctxt);
        LOGGER.debug("Reachability Statistics");
        LOGGER.debugf("  Reachable interfaces:          %s", info.interfaceHierarchy.size());
        LOGGER.debugf("  Reachable classes:             %s", info.classHierarchy.size());
        LOGGER.debugf("  Reachable functions:           %s", ctxt.numberEnqueued());
        LOGGER.debugf("  Dispatchable instance methods: %s", info.dispatchableMethods.size());
        LOGGER.debugf("  Invokable instance methods:    %s", info.invokableInstanceMethods.size());
        LOGGER.debugf("  Accessed static fields:        %s", info.accessedStaticField.size());
        ReachabilityRoots.get(ctxt).reportStats();
        info.analysis.reportStats();
    }

    // We force some fundamental types to be considered reachable even if the program doesn't use them.
    // This simplifies the implementation of the core runtime.
    public static void forceCoreClassesReachable(CompilationContext ctxt) {
        ReachabilityInfo info = get(ctxt);
        CoreClasses cc = CoreClasses.get(ctxt);
        LOGGER.debugf("Forcing all array types reachable/instantiated");
        String[] desc = { "[Z", "[B", "[C", "[S", "[I", "[F", "[J", "[D", "[ref" };
        LoadedTypeDefinition obj = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LoadedTypeDefinition cloneable = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Cloneable").load();
        LoadedTypeDefinition serializable = ctxt.getBootstrapClassContext().findDefinedType("java/io/Serializable").load();
        info.analysis.processInstantiatedClass(obj, false,null);
        info.addReachableInterface(cloneable);
        info.addReachableInterface(serializable);
        for (String d : desc) {
            LoadedTypeDefinition at = cc.getArrayLoadedTypeDefinition(d);
            info.addInterfaceEdge(at, cloneable);
            info.addInterfaceEdge(at, serializable);
            info.analysis.processInstantiatedClass(at,  false,null);
        }

        LOGGER.debugf("Forcing java.lang.Class reachable/instantiated");
        LoadedTypeDefinition clz = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        info.analysis.processInstantiatedClass(clz, false,null);

        LOGGER.debugf("Forcing jdk.internal.misc.Unsafe reachable/instantiated");
        LoadedTypeDefinition unsafe = ctxt.getBootstrapClassContext().findDefinedType("jdk/internal/misc/Unsafe").load();
        info.analysis.processInstantiatedClass(unsafe,  false,null);

        // The main Thread is instantiated in native code, and thus not visible to analysis.
        LOGGER.debugf("Forcing java.lang.Thread reachable/instantiated");
        LoadedTypeDefinition thr = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread").load();
        info.analysis.processInstantiatedClass(thr, false,null);
    }

    public static void processReachableElement(ExecutableElement elem) {
        ReachabilityInfo info = get(elem.getEnclosingType().getContext().getCompilationContext());
        info.processRootReachableElement(elem);
    }

    public void processRootReachableElement(ExecutableElement elem) {
        if (elem instanceof MethodElement me) {
            if (me.isStatic()) {
                analysis.processReachableType(me.getEnclosingType().load(), null);
                analysis.processReachableExactInvocation(me, null);
            } else {
                analysis.processReachableDispatchedInvocation(me, null);
            }
        } else if (elem instanceof ConstructorElement ce) {
            analysis.processInstantiatedClass(ce.getEnclosingType().load(), false, null);
            analysis.processReachableExactInvocation(ce, null);
        }
    }

    public boolean isDispatchableMethod(MethodElement meth) {
        return dispatchableMethods.contains(meth);
    }

    public boolean isInvokableInstanceMethod(MethodElement meth) {
        return invokableInstanceMethods.contains(meth);
    }

    public boolean isAccessedStaticField(FieldElement field) {
        return accessedStaticField.contains(field);
    }

    public boolean isReachableClass(LoadedTypeDefinition type) {
        return classHierarchy.containsKey(type);
    }

    public boolean isReachableInterface(LoadedTypeDefinition type) {
        return interfaceHierarchy.containsKey(type);
    }

    public void visitReachableInterfaces(Consumer<LoadedTypeDefinition> function) {
        for (LoadedTypeDefinition i : interfaceHierarchy.keySet()) {
            function.accept(i);
        }
    }

    public void visitReachableImplementors(LoadedTypeDefinition type, Consumer<LoadedTypeDefinition> function) {
        Set<LoadedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        Set<LoadedTypeDefinition> toProcess = new HashSet<>();
        collectImplementors(type, toProcess);
        for (LoadedTypeDefinition cls : toProcess) {
            function.accept(cls);
        }
    }

    private void collectImplementors(LoadedTypeDefinition type, Set<LoadedTypeDefinition> toProcess) {
        Set<LoadedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        for (LoadedTypeDefinition child : implementors) {
            toProcess.add(child);
            if (child.isInterface()) {
                collectImplementors(child, toProcess);
            } else {
                visitReachableSubclassesPreOrder(child, toProcess::add);
            }
        }
    }

    public void visitReachableSubclassesPreOrder(LoadedTypeDefinition type, Consumer<LoadedTypeDefinition> function) {
        Set<LoadedTypeDefinition> subclasses = classHierarchy.get(type);
        if (subclasses == null) return;
        for (LoadedTypeDefinition sc : subclasses) {
            function.accept(sc);
            visitReachableSubclassesPreOrder(sc, function);
        }
    }

    public void visitReachableSubclassesPostOrder(LoadedTypeDefinition type, Consumer<LoadedTypeDefinition> function) {
        Set<LoadedTypeDefinition> subclasses = classHierarchy.get(type);
        if (subclasses == null) return;
        for (LoadedTypeDefinition sc : subclasses) {
            visitReachableSubclassesPostOrder(sc, function);
            function.accept(sc);
        }
    }

    public void visitReachableTypes(Consumer<LoadedTypeDefinition> function) {
        for (LoadedTypeDefinition t: classHierarchy.keySet()) {
            function.accept(t);
        }
        for (LoadedTypeDefinition t: interfaceHierarchy.keySet()) {
            function.accept(t);
        }
    }

    /*
     * Package level methods, to allow a ReachabilityAnalysis to add methods/types to the info
     */

    ReachabilityAnalysis getAnalysis() {
        return analysis;
    }

    void addReachableInterface(LoadedTypeDefinition type) {
        if (isReachableInterface(type)) return;
        synchronized (this) {
            interfaceHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
            for (LoadedTypeDefinition i : type.getInterfaces()) {
                addReachableInterface(i);
                addInterfaceEdge(type, i);
            }

            // For every instance method that is not already dispatchable,
            // check to see if it has the same selector as an "overridden" dispatchable method.
            outer:
            for (MethodElement im : type.getInstanceMethods()) {
                if (!isDispatchableMethod(im)) {
                    for (LoadedTypeDefinition si : type.getInterfaces()) {
                        MethodElement sm = si.resolveMethodElementInterface(im.getName(), im.getDescriptor());
                        if (sm != null && isDispatchableMethod(sm)) {
                            LOGGER.debugf("\tnewly reachable interface: dispatchable method:  %s", im);
                            analysis.processReachableDispatchedInvocation(im, null);
                            continue outer;
                        }
                    }
                }
            }

            // The Class object of a reachable type is a heap root.
            analysis.processReachableObject(type.getVmClass(), null);
        }
    }

    private void addInterfaceEdge(LoadedTypeDefinition child, LoadedTypeDefinition parent) {
        interfaceHierarchy.computeIfAbsent(parent, t -> ConcurrentHashMap.newKeySet()).add(child);
    }

    void addReachableClass(LoadedTypeDefinition type) {
        if (isReachableClass(type)) return;
        synchronized (this) {
            classHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
            LoadedTypeDefinition superClass = type.getSuperClass();
            if (superClass != null) {
                addReachableClass(superClass);
                classHierarchy.get(superClass).add(type);
            }
            for (LoadedTypeDefinition i : type.getInterfaces()) {
                addReachableInterface(i);
                addInterfaceEdge(type, i);
            }
            // force class to be loaded (will fail if new reachable classes are discovered after ADD)
            type.getVmClass();

            // If I override a dispatchable superclass or interface method, make my version of that method dispatchable.
            methodLoop:
            for (MethodElement im : type.getInstanceMethods()) {
                if (!isDispatchableMethod(im)) {
                    if (type.hasSuperClass()) {
                        MethodElement overiddenMethod = type.getSuperClass().resolveMethodElementVirtual(im.getName(), im.getDescriptor());
                        if (overiddenMethod != null && isDispatchableMethod(overiddenMethod)) {
                            ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: dispatchable method %s from %s", im, type.getSuperClass());
                            analysis.processReachableDispatchedInvocation(im, null);
                            continue methodLoop;
                        }
                    }
                    for (LoadedTypeDefinition i : type.getInterfaces()) {
                        MethodElement sm = i.resolveMethodElementInterface(im.getName(), im.getDescriptor());
                        if (sm != null && isDispatchableMethod(sm)) {
                            ReachabilityInfo.LOGGER.debugf("\tnewly reachable class: dispatchable method: %s from %s", im, i);
                            analysis.processReachableDispatchedInvocation(im, null);
                            continue methodLoop;
                        }
                    }
                }
            }

            // The Class object of a reachable type is a heap root.
            analysis.processReachableObject(type.getVmClass(), null);
        }
    }

    void addReachableType(LoadedTypeDefinition type) {
        if (type.isInterface()) {
            addReachableInterface(type);
        } else {
            addReachableClass(type);
        }
    }

    void addDispatchableMethod(MethodElement meth) {
        if (dispatchableMethods.contains(meth)) return;
        synchronized (this) {
            addReachableType(meth.getEnclosingType().load());
            dispatchableMethods.add(meth);

            // First we must propagate dispatchability down the method family
            LoadedTypeDefinition definingClass = meth.getEnclosingType().load();

            if (definingClass.isInterface()) {
                // Traverse the reachable extenders and implementors and handle as-if we just saw
                // an invokevirtual/invokeinterface  of their overriding/implementing method
                visitReachableImplementors(definingClass, (c) -> {
                    MethodElement cand;
                    if (c.isInterface()) {
                        cand = c.resolveMethodElementInterface(meth.getName(), meth.getDescriptor());
                    } else {
                        cand = c.resolveMethodElementVirtual(meth.getName(), meth.getDescriptor());
                    }
                    if (cand != null && !isDispatchableMethod(cand)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly dispatchable method due to down propagation: %s", cand);
                        analysis.processReachableDispatchedInvocation(cand, null);
                    }
                });
            } else {
                // Traverse the instantiated subclasses of target's defining class and
                // ensure that all overriding implementations of this method are marked dispatchable.
                visitReachableSubclassesPreOrder(definingClass, (sc) -> {
                    MethodElement cand = sc.resolveMethodElementVirtual(meth.getName(), meth.getDescriptor());
                    if (cand != null && !isDispatchableMethod(cand)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly dispatchable method due to down propagation: %s", cand);
                        analysis.processReachableDispatchedInvocation(cand, null);
                    }
                });
                // To ensure compatible vtable layouts, we must also propagate dispatchability up the class hierarchy.
                // We do not have to propagate dispatchability up the interface hierarchy because the itable dispatch
                // mechanism does not have the strong identical-prefix requirements for superinterfaces that vtables do for superclasses.
                LoadedTypeDefinition ancestor = definingClass.getSuperClass();
                while (ancestor != null) {
                    MethodElement cand = ancestor.resolveMethodElementVirtual(meth.getName(), meth.getDescriptor());
                    if (cand != null && !isDispatchableMethod(cand)) {
                        ReachabilityInfo.LOGGER.debugf("\tnewly dispatchable method due to up propagation: %s", cand);
                        analysis.processReachableDispatchedInvocation(cand, null);
                    }
                    ancestor = ancestor.getSuperClass();
                }
            }
        }
    }

    void addInvokableInstanceMethod(MethodElement meth) {
        this.invokableInstanceMethods.add(meth);
    }

    void addAccessedStaticField(FieldElement field) {
        this.accessedStaticField.add(field);
    }
}
