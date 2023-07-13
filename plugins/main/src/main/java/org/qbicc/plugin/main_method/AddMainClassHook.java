package org.qbicc.plugin.main_method;

import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class AddMainClassHook implements Consumer<CompilationContext> {

    private static final String MAIN_CLASS_OLD = "jdk/internal/org/qbicc/runtime/Main";
    private static final String MAIN_CLASS = "jdk/internal/main/Main";

    public AddMainClassHook() {
    }

    public void accept(final CompilationContext ctxt) {
        MainMethod mainMethod = MainMethod.get(ctxt);
        String mainClassName = mainMethod.getMainClass();
        if (mainClassName != null) {
            String mainClassIntName = mainClassName.replace('.', '/');
            VmClassLoader appClassLoader = AppClassLoader.get(ctxt).getAppClassLoader();
            VmClass mainClass;
            try {
                mainClass = appClassLoader.loadClass(mainClassIntName);
            } catch (Throwable t) {
                ctxt.error("Failed to load user main class %s: %s", mainClassName, t);
                return;
            }
            LoadedTypeDefinition mainClassDef = mainClass.getTypeDefinition();
            int idx = mainClassDef.findMethodIndex(e -> {
                if (!e.getName().equals("main")) {
                    return false;
                }
                if (!e.hasAllModifiersOf(ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC)) {
                    return false;
                }
                MethodDescriptor desc = e.getDescriptor();
                if (desc.getReturnType() != BaseTypeDescriptor.V) {
                    return false;
                }
                List<TypeDescriptor> paramTypes = desc.getParameterTypes();
                if (paramTypes.size() != 1) {
                    return false;
                }
                TypeDescriptor paramType = paramTypes.get(0);
                if (! (paramType instanceof ArrayTypeDescriptor atd)) {
                    return false;
                }
                TypeDescriptor elementType = atd.getElementTypeDescriptor();
                if (! (elementType instanceof ClassTypeDescriptor classTypeDescriptor)) {
                    return false;
                }
                return classTypeDescriptor.packageAndClassNameEquals("java/lang", "String");
            });
            if (idx == -1) {
                ctxt.error("No valid main method found on \"%s\"", mainClassName);
                return;
            }
            MethodElement mainMethodElement = mainClassDef.getMethod(idx);
            UserMainIntrinsic.register(ctxt, mainMethodElement);
            // now, load and resolve the class with the real entry point on it, causing it to be registered
            DefinedTypeDefinition runtimeMain = ctxt.getBootstrapClassContext().findDefinedType(MAIN_CLASS);
            if (runtimeMain == null) {
                runtimeMain = ctxt.getBootstrapClassContext().findDefinedType(MAIN_CLASS_OLD);
                if (runtimeMain == null) {
                    ctxt.error("Unable to find runtime main class \"%s\"", MAIN_CLASS);
                    return;
                }
            }
            runtimeMain.load();
        }
    }
}
