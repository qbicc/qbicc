package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.impl.ConfigurationCondition;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.VerifyFailedException;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.TypeDescriptor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class QbiccRuntimeReflectionSupport implements RuntimeReflectionSupport {
    private final CompilationContext ctxt;
    final Set<Class<?>> registeredClasses = ConcurrentHashMap.newKeySet();
    final Set<Executable> registeredMethods = ConcurrentHashMap.newKeySet();
    final Set<Field> registeredFields = ConcurrentHashMap.newKeySet();

    QbiccRuntimeReflectionSupport(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public Map<Class<?>, Set<Class<?>>> getReflectionInnerClasses() {
        throw new UnsupportedOperationException();
    }

    public Set<Field> getReflectionFields() {
        throw new UnsupportedOperationException();
    }

    public Set<Executable> getReflectionExecutables() {
        throw new UnsupportedOperationException();
    }

    public Object getAccessor(Executable method) {
        throw new UnsupportedOperationException();
    }

    public Set<?> getHidingReflectionFields() {
        throw new UnsupportedOperationException();
    }

    public Set<?> getHidingReflectionMethods() {
        throw new UnsupportedOperationException();
    }

    public Set<Executable> getHiddenMethods() {
        throw new UnsupportedOperationException();
    }

    public Set<Executable> getQueriedOnlyMethods() {
        throw new UnsupportedOperationException();
    }

    public Object[] getRecordComponents(Class<?> type) {
        throw new UnsupportedOperationException();
    }

    public void registerHeapDynamicHub(Object hub) {
        throw new UnsupportedOperationException();
    }

    public Set<?> getHeapDynamicHubs() {
        throw new UnsupportedOperationException();
    }

    public void registerHeapReflectionObject(AccessibleObject object) {
        throw new UnsupportedOperationException();
    }

    public Set<AccessibleObject> getHeapReflectionObjects() {
        throw new UnsupportedOperationException();
    }

    public int getReflectionClassesCount() {
        throw new UnsupportedOperationException();
    }

    public int getReflectionMethodsCount() {
        throw new UnsupportedOperationException();
    }

    public int getReflectionFieldsCount() {
        throw new UnsupportedOperationException();
    }

    public boolean requiresProcessing() {
        throw new UnsupportedOperationException();
    }

    public void register(ConfigurationCondition condition, Class<?>... classes) {
        for (Class<?> c: classes) {
            registeredClasses.add(c);
        }
    }

    public void register(ConfigurationCondition condition, boolean unsafeAllocated, Class<?> clazz) {
        registeredClasses.add(clazz);
    }

    public void register(ConfigurationCondition condition, boolean queriedOnly, Executable... methods) {
        for (Executable e: methods) {
            registeredMethods.add(e);
        }
    }

    public void register(ConfigurationCondition condition, boolean finalIsWritable, Field... fields) {
        for (Field f: fields) {
            registeredFields.add(f);
        }
    }

    void processRegistrations() {
        ReachabilityRoots rr = ReachabilityRoots.get(ctxt);

        for (Class<?> clazz : registeredClasses) {
            LoadedTypeDefinition ltd = mapClass(clazz);
            if (ltd != null) {
                rr.registerReflectiveClass(ltd);
            }
        }

        for (Field f : registeredFields) {
            FieldElement fe = mapField(f);
            if (fe != null) {
                rr.registerReflectiveField(fe);
            }
        }

        for (Executable e : registeredMethods) {
            if (e instanceof Method m) {
                MethodElement me = mapMethod(m);
                if (me != null) {
                    rr.registerReflectiveMethod(me);
                }
            } else if (e instanceof Constructor c) {
                ConstructorElement ce = mapConstructor(c);
                if (ce != null) {
                    rr.registerReflectiveConstructor(ce);
                }
            }
        }
    }

    private LoadedTypeDefinition mapClass(Class<?> clazz) {
        VmClassLoader appCl = AppClassLoader.get(ctxt).getAppClassLoader();
        try {
            DefinedTypeDefinition dtd = appCl.getClassContext().findDefinedType(clazz.getName().replace(".", "/"));
            return dtd == null ? null: dtd.load();
        } catch (VerifyFailedException e) {
            ctxt.warning("Unable to load type %s due to VerifyFailedException %s", clazz.getName(), e.getMessage());
            return null;
        }
    }

    private MethodElement mapMethod(Method method) {
        LoadedTypeDefinition ltd = mapClass(method.getDeclaringClass());
        if (ltd == null) {
            return null;
        }
        int mi = ltd.findSingleMethodIndex(me -> {
            if (!me.getName().equals(method.getName()) ||
                me.isStatic() != Modifier.isStatic(method.getModifiers()) ||
                me.getParameters().size() != method.getParameterCount() ||
                me.hasAllModifiersOf(ClassFile.ACC_SYNTHETIC)) {
                return false;
            }
            Class<?>[] hostPtypes = method.getParameterTypes();
            List<TypeDescriptor> qbiccPTypes = me.getDescriptor().getParameterTypes();
            for (int i=0; i<hostPtypes.length; i++) {
                if (!hostPtypes[i].descriptorString().equals(qbiccPTypes.get(i).toString())) {
                    return false;
                }
            }
            return true;
        });
        return mi == -1 ? null : ltd.getMethod(mi);
    }

    private ConstructorElement mapConstructor(Constructor ctor) {
        LoadedTypeDefinition ltd = mapClass(ctor.getDeclaringClass());
        if (ltd == null) {
            return null;
        }
        int ci = ltd.findSingleConstructorIndex(ce -> {
            if (ce.getParameters().size() != ctor.getParameterCount()) {
                return false;
            }
            Class<?>[] hostPtypes = ctor.getParameterTypes();
            List<TypeDescriptor> qbiccPTypes = ce.getDescriptor().getParameterTypes();
            for (int i=0; i<hostPtypes.length; i++) {
                if (!hostPtypes[i].descriptorString().equals(qbiccPTypes.get(i).toString())) {
                    return false;
                }
            }
            return true;
        });
        return ci == -1 ? null : ltd.getConstructor(ci);
    }

    private FieldElement mapField(Field field) {
        LoadedTypeDefinition ltd = mapClass(field.getDeclaringClass());
        return ltd == null ? null : ltd.findField(field.getName());
    }
}
