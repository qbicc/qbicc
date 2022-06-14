package org.qbicc.plugin.reachability;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.TypeDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
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
    private final Set<FieldElement> accessedFields = ConcurrentHashMap.newKeySet();

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

    public void reportStats() {
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed classes: %s", accessedClasses.size());
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed methods: %s", accessedMethods.size());
        ReachabilityInfo.LOGGER.debugf("  Reflectively accessed fields:  %s", accessedFields.size());
    }

    public void registerClass(Class<?> clazz) {
        DefinedTypeDefinition dtd = ctxt.getBootstrapClassContext().findDefinedType(clazz.getName().replace(".", "/"));
        if (dtd != null) {
            accessedClasses.add(dtd.load());
        }
    }

    public void registerMethods(Executable... methods) {
        for (Executable e: methods) {
            Class<?> declaringClass = e.getDeclaringClass();
            DefinedTypeDefinition dtd = ctxt.getBootstrapClassContext().findDefinedType(declaringClass.getName().replace(".", "/"));
            if (dtd != null) {
                LoadedTypeDefinition ltd = dtd.load();
                if (e instanceof Constructor c) {
                    int ci = ltd.findSingleConstructorIndex(ce -> {
                        if (ce.getParameters().size() != c.getParameterCount()) {
                            return false;
                        }
                        Class<?>[] hostPtypes = c.getParameterTypes();
                        List<TypeDescriptor> qbiccPTypes = ce.getDescriptor().getParameterTypes();
                        for (int i=0; i<hostPtypes.length; i++) {
                            if (!hostPtypes[i].descriptorString().equals(qbiccPTypes.get(i).toString())) {
                                return false;
                            }
                        }
                        return true;
                    });
                    if (ci != -1) {
                        accessedMethods.add(ltd.getConstructor(ci));
                    }
                } else {
                    Method m = (Method) e;
                    int mi = ltd.findSingleMethodIndex(me -> {
                        if (!me.getName().equals(m.getName()) ||
                            me.isStatic() != Modifier.isStatic(m.getModifiers()) ||
                            me.getParameters().size() != m.getParameterCount()) {
                            return false;
                        }
                        Class<?>[] hostPtypes = m.getParameterTypes();
                        List<TypeDescriptor> qbiccPTypes = me.getDescriptor().getParameterTypes();
                        for (int i=0; i<hostPtypes.length; i++) {
                            if (!hostPtypes[i].descriptorString().equals(qbiccPTypes.get(i).toString())) {
                                return false;
                            }
                        }
                        return true;
                    });
                    if (mi != -1) {
                        accessedMethods.add(ltd.getMethod(mi));
                    }
                }
            }
        }
    }

    public void registerFields(Field... fields) {
        for (Field f: fields) {
            Class<?> declaringClass = f.getDeclaringClass();
            DefinedTypeDefinition dtd = ctxt.getBootstrapClassContext().findDefinedType(declaringClass.getName().replace(".", "/"));
            if (dtd != null) {
                LoadedTypeDefinition ltd = dtd.load();
                FieldElement fe = ltd.findField(f.getName());
                if (fe != null) {
                    accessedFields.add(fe);
                }
            }
        }
    }

    public static void makeReflectiveRootsReachable(CompilationContext ctxt) {
        RuntimeReflectionRoots rrr = get(ctxt);
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        for (LoadedTypeDefinition ltd : rrr.accessedClasses) {
            ReachabilityInfo.LOGGER.debugf("Reflectively accessed type %s made reachable", ltd.toString());
            info.getAnalysis().processReachableType(ltd, null);
        }
        for (ExecutableElement e : rrr.accessedMethods) {
            if (e instanceof MethodElement me) {
                if (me.isStatic()) {
                    info.getAnalysis().processReachableExactInvocation(me,null);
                } else {
                    info.getAnalysis().processReachableDispatchedInvocation(me, null);
                }
            } else if (e instanceof ConstructorElement ce) {
                info.getAnalysis().processInstantiatedClass(ce.getEnclosingType().load(), false, null);
                info.getAnalysis().processReachableExactInvocation(ce, null);
            }
        }
        for (FieldElement f : rrr.accessedFields) {
            if (f.isStatic()) {
                info.getAnalysis().processReachableStaticFieldAccess((StaticFieldElement) f, null);
            }
        }
    }
}
