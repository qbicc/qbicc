package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.impl.ConfigurationCondition;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class QbiccRuntimeReflectionSupport implements RuntimeReflectionSupport {
    private final CompilationContext ctxt;

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
        Arrays.stream(classes).forEach(clazz -> register(condition, false, clazz));
    }

    public void register(ConfigurationCondition condition, boolean unsafeAllocated, Class<?> clazz) {
        LoadedTypeDefinition ltd = ClassMapper.mapClass(ctxt, clazz);
        if (ltd != null) {
            ReachabilityRoots.get(ctxt).registerReflectiveClass(ltd);
        }
    }

    public void register(ConfigurationCondition condition, boolean queriedOnly, Executable... methods) {
        if (methods.length > 0) {
            ReachabilityRoots r = ReachabilityRoots.get(ctxt);
            for (Executable e : methods) {
                if (e instanceof Method m) {
                    MethodElement me = ClassMapper.mapMethod(ctxt, m);
                    if (me != null) {
                        r.registerReflectiveMethod(me);
                    }
                } else if (e instanceof Constructor c) {
                    ConstructorElement ce = ClassMapper.mapConstructor(ctxt, c);
                    if (ce != null) {
                        r.registerReflectiveConstructor(ce);
                    }
                }
            }
        }
    }

    public void register(ConfigurationCondition condition, boolean finalIsWritable, Field... fields) {
        if (fields.length > 0) {
            ReachabilityRoots r = ReachabilityRoots.get(ctxt);
            for (Field f : fields) {
                FieldElement fe = ClassMapper.mapField(ctxt, f);
                if (fe != null) {
                    r.registerReflectiveField(fe);
                }
            }
        }
    }
}
