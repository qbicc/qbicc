package org.qbicc.plugin.reachability;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Load;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks runtime-reflection operations and ensures that the necessary
 * Classes, Methods, etc. are preserved in the final native image.
 */
public class RuntimeReflectionRoots {
    private static final AttachmentKey<RuntimeReflectionRoots> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<LoadedTypeDefinition> accessedClasses = ConcurrentHashMap.newKeySet();
    private final Set<ExecutableElement> accessedMethods = ConcurrentHashMap.newKeySet();

    private RuntimeReflectionRoots(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static RuntimeReflectionRoots get(CompilationContext ctxt) {
        RuntimeReflectionRoots info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new RuntimeReflectionRoots(ctxt);
            RuntimeReflectionRoots appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public void registerClass(Class<?> clazz) {
        DefinedTypeDefinition dtd = ctxt.getBootstrapClassContext().findDefinedType(clazz.getName().replace(".", "/"));
        if (dtd != null) {
            accessedClasses.add(dtd.load());
        }
    }

    public static void makeReflectiveRootsReachable(CompilationContext ctxt) {
        RuntimeReflectionRoots rrr = get(ctxt);
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        for (LoadedTypeDefinition ltd : rrr.accessedClasses) {
            ReachabilityInfo.LOGGER.debugf("Reflectively accessed %s made reachable", ltd.toString());
            info.getAnalysis().processReachableType(ltd, null);
        }
    }

}
