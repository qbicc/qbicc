package org.qbicc.plugin.reachability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * RTAInfo tracks the types and methods that have been determined to be reachable
 * and/or instantiatable from a program entrypoint.
 *
 * A class or interface is called reachable if a reference to a
 * static program element it defines or its java.lang.Class instance
 * could be executed by an invokable method.
 *
 * A class is called instantiated if a `new` of the class (or one of its subclasses)
 * could be executed by an invokable method.
 *
 * A reachable static method is considered to be invokable.
 * A reachable interface method is considered to be invokable.
 * A reachable instance method of a class is only considered to be invokable if its defining class is instantiated.
 *
 * If a class is reachable, its superclass is also considered to be reachable.
 * Class reachability does not imply reachability of its implemented interfaces.
 *
 * If a class is instantiated, then its superclass is also considered to be instantiated.
 * If a class is instantiated, then both its directly implemented interfaces and all of their
 * ancestor interfaces are considered to be reachable.
 * An instantiated class is always also a reachable class.
 *
 * All classes and interfaces that are reachable after the ANALYZE phase completes
 * will be assigned typeIds.
 *
 * All methods that are invokable at the end of the ANALYZE phase
 * will be carried through the rest of the phases and compiled.
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
public class RTAInfo {
    static final Logger rtaLog = Logger.getLogger("org.qbicc.plugin.reachability.rta");
    private static final AttachmentKey<RTAInfo> KEY = new AttachmentKey<>();

    // Tracks reachable classes and their (direct) reachable subclasses
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> classHierarchy = new ConcurrentHashMap<>();
    // Tracks reachable interfaces and their (direct) reachable implementors
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> interfaceHierarchy = new ConcurrentHashMap<>();
    // Tracks actually instantiated classes
    private final Set<LoadedTypeDefinition> instantiatedClasses = ConcurrentHashMap.newKeySet();
    // Tracks classes and interfaces whose <clinit> could be invoked at runtime
    private final Set<LoadedTypeDefinition> initializedTypes = ConcurrentHashMap.newKeySet();
    // Set of interfaces whose META-INF files have been searched for service implementations
    private final Set<LoadedTypeDefinition> searchedServiceInterfaces = ConcurrentHashMap.newKeySet();

    // Set of invokable instance methods
    private final Set<MethodElement> invokableMethods = ConcurrentHashMap.newKeySet();
    // Set of reachable, but not yet invokable, instance methods
    private final Set<MethodElement> deferredInstanceMethods = ConcurrentHashMap.newKeySet();

    private final CompilationContext ctxt;

    private RTAInfo(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static RTAInfo get(CompilationContext ctxt) {
        RTAInfo info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new RTAInfo(ctxt);
            RTAInfo appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public static void clear(CompilationContext ctxt) {
        RTAInfo info = get(ctxt);
        rtaLog.debugf("Clearing RTAInfo %s classes; %s interfaces", info.classHierarchy.size(), info.interfaceHierarchy.size());
        info.classHierarchy.clear();
        info.interfaceHierarchy.clear();
        info.instantiatedClasses.clear();
        info.initializedTypes.clear();
        info.searchedServiceInterfaces.clear();
        info.invokableMethods.clear();
        info.deferredInstanceMethods.clear();
    }

    // We force some fundamental types to be considered reachable even if the program doesn't use them.
    // This simplifies the implementation of the core runtime.
    public static void forceCoreClassesReachable(CompilationContext ctxt) {
        RTAInfo info = get(ctxt);
        Layout layout = Layout.get(ctxt);
        rtaLog.debugf("Forcing all array types reachable/instantiated");
        String[] desc = { "[Z", "[B", "[C", "[S", "[I", "[F", "[J", "[D", "[ref" };
        LoadedTypeDefinition obj = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LoadedTypeDefinition cloneable = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Cloneable").load();
        LoadedTypeDefinition serializable = ctxt.getBootstrapClassContext().findDefinedType("java/io/Serializable").load();
        info.processClassInitialization(obj);
        info.processInstantiatedClass(obj, true, null);
        info.addReachableInterface(cloneable);
        info.addReachableInterface(serializable);
        for (String d : desc) {
            LoadedTypeDefinition at = layout.getArrayLoadedTypeDefinition(d);
            info.addInterfaceEdge(at, cloneable);
            info.addInterfaceEdge(at, serializable);
            info.addInitializedType(at);
            info.processInstantiatedClass(at, true, null);
        }

        rtaLog.debugf("Forcing java.lang.Class reachable/instantiated");
        LoadedTypeDefinition clz = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        info.processClassInitialization(clz);
        info.processInstantiatedClass(clz, true, null);

        rtaLog.debugf("Forcing jdk.internal.misc.Unsafe reachable/instantiated");
        LoadedTypeDefinition unsafe = ctxt.getBootstrapClassContext().findDefinedType("jdk/internal/misc/Unsafe").load();
        info.processClassInitialization(unsafe);
        info.processInstantiatedClass(unsafe, true, null);

        // Hack around the way NoGC entrypoints are registered and then not used until LOWERING PHASE...
        LoadedTypeDefinition nogc = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/gc/nogc/NoGcHelpers").load();
        info.processClassInitialization(nogc);
    }

    public boolean isDeferredInstanceMethod(MethodElement meth) {
        return deferredInstanceMethods.contains(meth);
    }

    public boolean isInvokableMethod(MethodElement meth) {
        return invokableMethods.contains(meth);
    }

    public boolean isReachableClass(LoadedTypeDefinition type) {
        return classHierarchy.containsKey(type);
    }

    public boolean isReachableInterface(LoadedTypeDefinition type) {
        return interfaceHierarchy.containsKey(type);
    }

    public boolean isInitializedType(LoadedTypeDefinition type) {
        return initializedTypes.contains(type);
    }

    public boolean isInstantiatedClass(LoadedTypeDefinition type) {
        return instantiatedClasses.contains(type);
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

    public void visitInitializedTypes(Consumer<LoadedTypeDefinition> function) {
        for (LoadedTypeDefinition t: initializedTypes) {
            function.accept(t);
        }
    }

    /*
     * Entrypoints for Rapid Type Analysis algorithm, only meant to be invoked from ReachabilityBlockBuilder
     */
    synchronized void processArrayElementType(ObjectType elemType) {
        if (elemType instanceof ClassObjectType) {
            addReachableClass(elemType.getDefinition().load());
        } else if (elemType instanceof InterfaceObjectType) {
            addReachableInterface(elemType.getDefinition().load());
        }
    }

    synchronized void processReachableInstanceMethodInvoke(final MethodElement target, ExecutableElement originalElement) {
        if (!isInvokableMethod(target)) {
            LoadedTypeDefinition definingClass = target.getEnclosingType().load();
            if (definingClass.isInterface() || isInstantiatedClass(definingClass)) {
                rtaLog.debugf("Adding method %s (directly invoked in %s)", target, originalElement);
                invokableMethods.add(target);
                ctxt.enqueue(target);
                if (!target.isPrivate()) {
                    propagateInvokabilityToOverrides(target);
                }
            } else {
                rtaLog.debugf("Deferring method %s (invoked in %s, but no instantiated receiver)", target, originalElement);
                addReachableClass(definingClass);
                deferredInstanceMethods.add(target);
            }
        }
    }

    synchronized void processStaticElementInitialization(final LoadedTypeDefinition ltd) {
        if (isInitializedType(ltd)) return;
        if (ltd.isInterface()) {
            addReachableInterface(ltd);
            // JLS: accessing a static field/method of an interface only causes local <clinit> execution
            addInitializedType(ltd);
            if (ltd.getInitializer() != null) {
                ctxt.enqueue(ltd.getInitializer());
            }
        } else {
            // JLS: accessing a static field/method of a class <clinit> all the way up the class/interface hierarchy
            processClassInitialization(ltd);
        }
    }

    synchronized void processClassInitialization(final LoadedTypeDefinition ltd) {
        Assert.assertFalse(ltd.isInterface());
        if (isInitializedType(ltd)) return;
        addReachableClass(ltd);

        if (ltd.hasSuperClass()) {
            // force superclass initialization
            processClassInitialization(ltd.getSuperClass());
        }
        addInitializedType(ltd);

        if (ltd.getInitializer() != null) {
            ctxt.enqueue(ltd.getInitializer());
        }

        // Annoyingly, because an intermediate interface could be marked initialized due to a static field
        // access which doesn't cause the initialization of its superinterfaces, we can't short-circuit
        // this walk up the entire interface hierarchy when we hit an already initialized interfaces.
        ArrayDeque<LoadedTypeDefinition> worklist = new ArrayDeque<>(List.of(ltd.getInterfaces()));
        while (!worklist.isEmpty()) {
            LoadedTypeDefinition i = worklist.pop();
            if (i.declaresDefaultMethods() && !isInitializedType(i)) {
                addInitializedType(i);
                if (i.getInitializer() != null) {
                    ctxt.enqueue(i.getInitializer());
                }
            }
            worklist.addAll(List.of(i.getInterfaces()));
        }
    }

    synchronized void processInstantiatedClass(final LoadedTypeDefinition type, boolean directlyInstantiated, ExecutableElement originalElement) {
        if (isInstantiatedClass(type)) return;

        if (directlyInstantiated) {
            rtaLog.debugf("Adding class %s (instantiated in %s)", type.getDescriptor().getClassName(), originalElement);
        } else {
            rtaLog.debugf("\tadding ancestor class: %s", type.getDescriptor().getClassName());
        }
        addReachableClass(type);
        instantiatedClasses.add(type);

        // It's critical that we recur to handle our superclass first.  That means all of its invokable/deferred methods
        // that we override will be processed before we process our own defined instance methods below.
        if (type.hasSuperClass()) {
            processInstantiatedClass(type.getSuperClass(), false, originalElement);
        }

        // TODO: Now that we are explicitly tracking directly invoked deferred methods, we might be able to
        //       replace the checks below for overridding an invokable method with logic that instead
        //       adds overridden invoked methods to the deferred set in addReachableClass and processInvokableInstanceMethod
        //       It's not clear yet which of these options is simpler/more efficient/more maintainable...

        // For every instance method that is not already invokable,
        // check to see if it is either (a) a deferred invoked method or (b) overriding an invokable method and thus should be enqueued.
        for (MethodElement im : type.getInstanceMethods()) {
            if (!isInvokableMethod(im)) {
                if (isDeferredInstanceMethod(im)) {
                    rtaLog.debugf("\tnewly reachable class: enqueued deferred instance method: %s", im);
                    deferredInstanceMethods.remove(im);
                    invokableMethods.add(im);
                    ctxt.enqueue(im);
                } else if (type.hasSuperClass()) {
                    MethodElement overiddenMethod = type.getSuperClass().resolveMethodElementVirtual(im.getName(), im.getDescriptor());
                    if (overiddenMethod != null && isInvokableMethod(overiddenMethod)) {
                        rtaLog.debugf("\tnewly reachable class: enqueued overriding instance method: %s", im);
                        invokableMethods.add(im);
                        ctxt.enqueue(im);
                    }
                }
            }
        }

        // For every invokable interface method, make sure my implementation of that method is invokable.
        for (LoadedTypeDefinition i : type.getInterfaces()) {
            for (MethodElement sig : i.getInstanceMethods()) {
                if (isInvokableMethod(sig)) {
                    MethodElement impl = type.resolveMethodElementVirtual(sig.getName(), sig.getDescriptor());
                    if (impl != null && !isInvokableMethod(impl)) {
                        rtaLog.debugf("\tnewly reachable class: enqueued implementing method:  %s", impl);
                        invokableMethods.add(impl);
                        deferredInstanceMethods.remove(impl); // might not be deferred, but remove is a no-op if it isn't present
                        ctxt.enqueue(impl);
                    }
                }
            }
        }
    }

    synchronized void processServiceInterface(final LoadedTypeDefinition ltd, final String internalName) {
        if (ltd.isInterface() && !isServiceSearched(ltd)) {
            /* process service interface */
            addReachableInterface(ltd);

            /* find and load service implementations */
            String fileName = "META-INF/services/" + internalName.replace("/", ".");
            try {
                Enumeration<URL> servicefiles = ClassLoader.getSystemResources(fileName);
                while (servicefiles.hasMoreElements()) {
                    URL file = servicefiles.nextElement();
                    BufferedReader in = new BufferedReader(new InputStreamReader(file.openStream()));

                    String line;
                    while ((line = in.readLine()) != null) {
                        if (!line.isBlank()) {
                            DefinedTypeDefinition serviceImplDefinition = ctxt.getBootstrapClassContext().findDefinedType(line.replace(".", "/"));
                            LoadedTypeDefinition serviceImpl = serviceImplDefinition.load();
                            addReachableClass(serviceImpl);
                            addServiceMethods(serviceImpl);
                        }
                    }
                    in.close();

                }
                addSearchedServiceInterface(ltd);
            } catch(IOException e) {
                ctxt.error(e, "Failed to read " + fileName);
            }
        }
    }

    /*
     * RTA Helper methods.
     */

    private boolean isServiceSearched(LoadedTypeDefinition ltd) {
        return searchedServiceInterfaces.contains(ltd);
    }

    private void addSearchedServiceInterface(LoadedTypeDefinition ltd) {
        searchedServiceInterfaces.add(ltd);
    }

    private void addServiceMethods(LoadedTypeDefinition ltd) {
        for (int i = 0; i < ltd.getMethodCount(); i++) {
            MethodElement method = ltd.getMethod(i);
            invokableMethods.add(method);
            ctxt.enqueue(method);
        }
    }

    private void addReachableInterface(LoadedTypeDefinition type) {
        if (isReachableInterface(type)) return;
        interfaceHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
        for (LoadedTypeDefinition i: type.getInterfaces()) {
            addReachableInterface(i);
            addInterfaceEdge(type, i);
        }

        // For every instance method that is not already invokable,
        // check to see if it has the same selector as an "overridden" invokable method.
        outer:
        for (MethodElement im : type.getInstanceMethods()) {
            if (!isInvokableMethod(im)) {
                for (LoadedTypeDefinition si : type.getInterfaces()) {
                    MethodElement sm = si.resolveMethodElementInterface(im.getName(), im.getDescriptor());
                    if (sm != null && isInvokableMethod(sm)) {
                        rtaLog.debugf("\tnewly reachable interface: enqueued implementing method:  %s", im);
                        invokableMethods.add(im);
                        ctxt.enqueue(im);
                        continue outer;
                    }
                }
            }
        }
    }

    private void addInterfaceEdge(LoadedTypeDefinition child, LoadedTypeDefinition parent) {
        interfaceHierarchy.computeIfAbsent(parent, t -> ConcurrentHashMap.newKeySet()).add(child);
    }

    private void addReachableClass(LoadedTypeDefinition type) {
        if (isReachableClass(type)) return;
        classHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
        LoadedTypeDefinition superClass = type.getSuperClass();
        if (superClass != null) {
            addReachableClass(superClass);
            classHierarchy.get(superClass).add(type);
        }
        for (LoadedTypeDefinition i: type.getInterfaces()) {
            addReachableInterface(i);
            addInterfaceEdge(type, i);
        }
    }

    private void addInitializedType(LoadedTypeDefinition type) {
        if (isInitializedType(type)) return;
        if (type.isInterface()) {
            addReachableInterface(type);
        } else {
            addReachableClass(type);
        }
        initializedTypes.add(type);
    }

    private void propagateInvokabilityToOverrides(final MethodElement target) {
        LoadedTypeDefinition definingClass = target.getEnclosingType().load();

        if (definingClass.isInterface()) {
            // Traverse the reachable extenders and implementors and handle as-if we just saw
            // an invokevirtual/invokeinterface  of their overriding/implementing method
            visitReachableImplementors(definingClass, (c) -> {
                MethodElement cand = null;
                if (c.isInterface()) {
                    cand = c.resolveMethodElementInterface(target.getName(), target.getDescriptor());
                } else if (isInstantiatedClass(c)) {
                    cand = c.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                }
                if (cand != null && !isInvokableMethod(cand)) {
                    rtaLog.debugf("\tadding method (implements): %s", cand);
                    invokableMethods.add(cand);
                    ctxt.enqueue(cand);
                    propagateInvokabilityToOverrides(cand);
                }
            });
        } else {
            // Traverse the instantiated subclasses of target's defining class and
            // ensure that all overriding implementations of this method are marked invokable.
            visitReachableSubclassesPreOrder(definingClass, (sc) -> {
                if (isInstantiatedClass(sc)) {
                    MethodElement cand = sc.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
                    if (!isInvokableMethod(cand)) {
                        rtaLog.debugf("\tadding method (subclass overrides): %s", cand);
                        invokableMethods.add(cand);
                        ctxt.enqueue(cand);
                    }
                }
            });
        }
    }
}
