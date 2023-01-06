package org.qbicc.plugin.reachability;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 * Tracks the Classes, Methods, Constructors, and Fields that
 * are not native Entrypoints, but need to be treated as-if they
 * were spontaneously callable for the purpose of ReachabilityAnalysis.
 * These program elements generally fall into two broad categories:
 *   1. Elements that are accessed reflectively
 *   2. Elements that are part of the qbicc runtime and will be
 *      accessed by code expansions that happen after the ADD phase.
 *
 * A key different between this class and ReachabilityInfo is that it
 * is not cleared between phases.  Thus, once a program element is
 * registered as being a ReachabilityRoot, it is guaranteed to be
 * considered reachable for the rest of the compilation.
 */
public class ReachabilityRoots {
    private static final AttachmentKey<ReachabilityRoots> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<LoadedTypeDefinition> reflectiveClasses = ConcurrentHashMap.newKeySet();
    private final Set<ExecutableElement> reflectiveMethods = ConcurrentHashMap.newKeySet();
    private final Set<FieldElement> reflectiveFields = ConcurrentHashMap.newKeySet();
    private final Set<ExecutableElement> autoQueuedMethods = ConcurrentHashMap.newKeySet();
    private final Set<ExecutableElement> dispatchTableMethods = ConcurrentHashMap.newKeySet();

    private ReachabilityRoots(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static ReachabilityRoots get(CompilationContext ctxt) {
        ReachabilityRoots info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new ReachabilityRoots(ctxt);
            ReachabilityRoots appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public void reportStats() {
        ReachabilityInfo.LOGGER.debugf("  Auto-queued methods:           %s", autoQueuedMethods.size());
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed classes: %s", reflectiveClasses.size());
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed methods: %s", reflectiveMethods.size());
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed fields:  %s", reflectiveFields.size());
    }

    public void registerReflectiveClass(LoadedTypeDefinition ltd) {
        reflectiveClasses.add(ltd);
    }

    public boolean registerReflectiveMethod(MethodElement e) {
        boolean added = reflectiveMethods.add(e);
        if (added) {
            ctxt.enqueue(e);
        }
        return added;
    }

    public boolean registerReflectiveConstructor(ConstructorElement e) {
        boolean added = reflectiveMethods.add(e);
        if (added) {
            ctxt.enqueue(e);
        }
        return added;
    }

    public boolean registerAutoQueuedElement(ExecutableElement e) {
        boolean added = autoQueuedMethods.add(e);
        if (added) {
            ctxt.enqueue(e);
        }
        return added;
    }

    public boolean registerDispatchTableEntry(ExecutableElement e) {
        return dispatchTableMethods.add(e);
    }

    public boolean registerReflectiveField(FieldElement f) {
        return reflectiveFields.add(f);
    }

    // During Analyze, we can allow reachability analysis to figure things out for us.
    public static void processRootsForAnalyze(CompilationContext ctxt) {
        ReachabilityRoots roots = get(ctxt);
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        for (ExecutableElement e : roots.autoQueuedMethods) {
            info.processRootReachableElement(e);
        }
        for (LoadedTypeDefinition ltd : roots.reflectiveClasses) {
            info.getAnalysis().processReachableType(ltd, null);
        }
        for (ExecutableElement e : roots.reflectiveMethods) {
            info.processRootReachableElement(e);
        }
        for (FieldElement f : roots.reflectiveFields) {
            if (f.isStatic()) {
                info.getAnalysis().processReachableStaticFieldAccess((StaticFieldElement) f, null);
            }
        }
    }

    // During Lower, we don't have to handle instance methods in the autoQueued or reflective sets.
    // These methods are either in the dispatchTable set, or reachability analysis has determined that
    // there cannot be an instantiated class that could invoke the method, and therefore it is not reachable.
    public static void processRootsForLower(CompilationContext ctxt) {
        ReachabilityRoots roots = get(ctxt);
        for (ExecutableElement e : roots.autoQueuedMethods) {
            if (e.isStatic()) {
                ctxt.enqueue(e);
            }
        }
        for (ExecutableElement e : roots.reflectiveMethods) {
            if (e.isStatic()) {
                ctxt.enqueue(e);
            }
        }
        for (ExecutableElement e : roots.dispatchTableMethods) {
            ctxt.enqueue(e);
        }
    }

    public boolean isReflectiveClass(LoadedTypeDefinition ltd) {
        return reflectiveClasses.contains(ltd);
    }

    public Set<LoadedTypeDefinition> getReflectiveClasses() {
        return new HashSet<>(reflectiveClasses);
    }

    public Set<FieldElement> getReflectiveFields() {
        return new HashSet<>(reflectiveFields);
    }

    public Set<ExecutableElement> getReflectiveMethods() {
        return new HashSet<>(reflectiveMethods);
    }
}
