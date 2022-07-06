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
        return reflectiveMethods.add(e);
    }

    public boolean registerReflectiveConstructor(ConstructorElement e) {
        return reflectiveMethods.add(e);
    }

    public boolean registerAutoQueuedElement(ExecutableElement e) {
        return autoQueuedMethods.add(e);
    }

    public boolean registerDispatchTableEntry(ExecutableElement e) {
        return dispatchTableMethods.add(e);
    }

    public boolean registerReflectiveField(FieldElement f) {
        return reflectiveFields.add(f);
    }

    public static void processReachabilityRoots(CompilationContext ctxt) {
        ReachabilityRoots roots = get(ctxt);
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        for (ExecutableElement e : roots.autoQueuedMethods) {
            info.processRootReachableElement(e);
        }
        for (LoadedTypeDefinition ltd : roots.reflectiveClasses) {
            ReachabilityInfo.LOGGER.debugf("Reflectively accessed type %s made reachable", ltd.toString());
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

    public static void enqueueReachabilityRoots(CompilationContext ctxt) {
        ReachabilityRoots roots = get(ctxt);
        for (ExecutableElement e : roots.autoQueuedMethods) {
            ctxt.enqueue(e);
        }
        for (ExecutableElement e : roots.reflectiveMethods) {
            ctxt.enqueue(e);
        }
        for (ExecutableElement e : roots.dispatchTableMethods) {
            ctxt.enqueue(e);
        }
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
