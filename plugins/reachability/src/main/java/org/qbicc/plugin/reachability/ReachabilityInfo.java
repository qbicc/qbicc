package org.qbicc.plugin.reachability;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

/**
 * ReachabilityInfo tracks the types and methods that have been determined to be reachable
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
 */
public class ReachabilityInfo {
    static final Logger LOGGER = Logger.getLogger("org.qbicc.plugin.reachability");
    private static final AttachmentKey<ReachabilityInfo> KEY = new AttachmentKey<>();

    // Tracks reachable classes and their (direct) reachable subclasses
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> classHierarchy = new ConcurrentHashMap<>();
    // Tracks reachable interfaces and their (direct) reachable implementors
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> interfaceHierarchy = new ConcurrentHashMap<>();
    // Tracks actually instantiated classes
    private final Set<LoadedTypeDefinition> instantiatedClasses = ConcurrentHashMap.newKeySet();
    // Tracks classes and interfaces whose <clinit> could be invoked at runtime
    private final Set<LoadedTypeDefinition> initializedTypes = ConcurrentHashMap.newKeySet();

    // Set of invokable instance methods
    private final Set<MethodElement> invokableMethods = ConcurrentHashMap.newKeySet();

    private final ReachabilityAnalysis analysis;

    private final CompilationContext ctxt;

    private ReachabilityInfo(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        // TODO: Currently hardwired to RTA; eventually will support multiple analysis algorithms
        this.analysis = new RapidTypeAnalysis(this, ctxt);
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
        info.instantiatedClasses.clear();
        info.initializedTypes.clear();
        info.invokableMethods.clear();
        info.analysis.clear();
    }

    public static void reportStats(CompilationContext ctxt) {
        ReachabilityInfo info = get(ctxt);
        LOGGER.debug("Reachability Statistics");
        LOGGER.debugf("  Reachable interfaces:       %s", info.interfaceHierarchy.size());
        LOGGER.debugf("  Reachable classes:          %s", info.classHierarchy.size());
        LOGGER.debugf("  Instantiated classes:       %s", info.instantiatedClasses.size());
        LOGGER.debugf("  Initialized types:          %s", info.initializedTypes.size());
        LOGGER.debugf("  Invokable instance methods: %s", info.invokableMethods.size());
        info.analysis.reportStats();
    }

    // We force some fundamental types to be considered reachable even if the program doesn't use them.
    // This simplifies the implementation of the core runtime.
    public static void forceCoreClassesReachableBuildTimeInit(CompilationContext ctxt) {
        forceCoreClassesReachable(ctxt, true);
    }

    public static void forceCoreClassesReachableRunTimeInit(CompilationContext ctxt) {
        forceCoreClassesReachable(ctxt, false);
    }

    private static void forceCoreClassesReachable(CompilationContext ctxt, boolean buildTimeInit) {
        ReachabilityInfo info = get(ctxt);
        CoreClasses cc = CoreClasses.get(ctxt);
        LOGGER.debugf("Forcing all array types reachable/instantiated");
        String[] desc = { "[Z", "[B", "[C", "[S", "[I", "[F", "[J", "[D", "[ref" };
        LoadedTypeDefinition obj = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        LoadedTypeDefinition cloneable = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Cloneable").load();
        LoadedTypeDefinition serializable = ctxt.getBootstrapClassContext().findDefinedType("java/io/Serializable").load();
        info.analysis.processInstantiatedClass(obj, true, false,null);
        info.analysis.processClassInitialization(obj, buildTimeInit);
        info.addReachableInterface(cloneable);
        info.addReachableInterface(serializable);
        for (String d : desc) {
            LoadedTypeDefinition at = cc.getArrayLoadedTypeDefinition(d);
            info.addInterfaceEdge(at, cloneable);
            info.addInterfaceEdge(at, serializable);
            info.addInitializedType(at);
            info.analysis.processInstantiatedClass(at, true, false,null);
        }

        LOGGER.debugf("Forcing java.lang.Class reachable/instantiated");
        LoadedTypeDefinition clz = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        info.analysis.processInstantiatedClass(clz, true, false,null);
        info.analysis.processClassInitialization(clz, buildTimeInit);

        LOGGER.debugf("Forcing jdk.internal.misc.Unsafe reachable/instantiated");
        LoadedTypeDefinition unsafe = ctxt.getBootstrapClassContext().findDefinedType("jdk/internal/misc/Unsafe").load();
        info.analysis.processInstantiatedClass(unsafe, true, false,null);
        info.analysis.processClassInitialization(unsafe, buildTimeInit);

        // Hack around the way NoGC entrypoints are registered and then not used until LOWERING PHASE...
        LOGGER.debugf("Forcing org.qbicc.runtime.gc.nogc.NoGcHelpers reachable");
        LoadedTypeDefinition nogc = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/gc/nogc/NoGcHelpers").load();
        info.analysis.processClassInitialization(nogc, buildTimeInit);
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
     * Package level methods, to allow a ReachabilityAnalysis to add methods/types to the info
     */

    ReachabilityAnalysis getAnalysis() {
        return analysis;
    }

    void addReachableInterface(LoadedTypeDefinition type) {
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
                        LOGGER.debugf("\tnewly reachable interface: enqueued implementing method:  %s", im);
                        invokableMethods.add(im);
                        ctxt.enqueue(im);
                        continue outer;
                    }
                }
            }
        }
    }
    void addInterfaceEdge(LoadedTypeDefinition child, LoadedTypeDefinition parent) {
        interfaceHierarchy.computeIfAbsent(parent, t -> ConcurrentHashMap.newKeySet()).add(child);
    }

    void addReachableClass(LoadedTypeDefinition type) {
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
        // force class to be loaded (will fail if new reachable classes are discovered after ADD)
        type.getVmClass();
    }

    void addInstantiatedClass(LoadedTypeDefinition type) {
        instantiatedClasses.add(type);
    }

    void addInitializedType(LoadedTypeDefinition type) {
        if (isInitializedType(type)) return;
        if (type.isInterface()) {
            addReachableInterface(type);
        } else {
            addReachableClass(type);
        }
        initializedTypes.add(type);
    }

    void addInvokableMethod(MethodElement meth) {
        this.invokableMethods.add(meth);
    }
}
