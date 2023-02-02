package org.qbicc.plugin.reachability;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
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
    private final Set<ExecutableElement> reflectiveMethods = ConcurrentHashMap.newKeySet();
    private final Set<StaticFieldElement> heapRoots = ConcurrentHashMap.newKeySet();
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
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed methods: %s", reflectiveMethods.size());
        ReachabilityInfo.LOGGER.debugf("  Reflective heap roots (statics): %s", heapRoots.size());
    }

    public boolean registerReflectiveEntrypoint(ExecutableElement e) {
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

    public boolean registerHeapRoot(StaticFieldElement f) {
        return heapRoots.add(f);
    }

    // During Analyze, we can allow reachability analysis to figure things out for us.
    public static void processRootsForAnalyze(CompilationContext ctxt) {
        ReachabilityRoots roots = get(ctxt);
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        for (ExecutableElement e : roots.autoQueuedMethods) {
            info.processRootReachableElement(e);
        }
        for (ExecutableElement e : roots.reflectiveMethods) {
            info.processRootReachableElement(e);
        }
        for (StaticFieldElement f : roots.heapRoots) {
            info.getAnalysis().processReachableStaticFieldAccess(f, null);
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
}
