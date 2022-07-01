package org.qbicc.plugin.nativeimage;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.VerifyFailedException;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.TypeDescriptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

class ClassMapper {

    static LoadedTypeDefinition mapClass(CompilationContext ctxt, Class<?> clazz) {
        VmClassLoader appCl = AppClassLoader.get(ctxt).getAppClassLoader();
        try {
            DefinedTypeDefinition dtd = appCl.getClassContext().findDefinedType(clazz.getName().replace(".", "/"));
            return dtd == null ? null: dtd.load();
        } catch (VerifyFailedException e) {
            ctxt.warning("Unable to load type %s due to VerifyFailedException %s", clazz.getName(), e.getMessage());
            return null;
        }
    }

    static MethodElement mapMethod(CompilationContext ctxt, Method method) {
        LoadedTypeDefinition ltd = mapClass(ctxt, method.getDeclaringClass());
        if (ltd == null) {
            return null;
        }
        int mi = ltd.findSingleMethodIndex(me -> {
            if (!me.getName().equals(method.getName()) ||
                me.isStatic() != Modifier.isStatic(method.getModifiers()) ||
                me.getParameters().size() != method.getParameterCount()) {
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

    static ConstructorElement mapConstructor(CompilationContext ctxt, Constructor ctor) {
        LoadedTypeDefinition ltd = mapClass(ctxt, ctor.getDeclaringClass());
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

    static FieldElement mapField(CompilationContext ctxt, Field field) {
        LoadedTypeDefinition ltd = mapClass(ctxt, field.getDeclaringClass());
        return ltd == null ? null : ltd.findField(field.getName());
    }
}
