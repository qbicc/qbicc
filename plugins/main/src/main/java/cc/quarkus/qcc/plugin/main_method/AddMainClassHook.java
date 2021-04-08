package cc.quarkus.qcc.plugin.main_method;

import java.util.List;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class AddMainClassHook implements Consumer<CompilationContext> {

    private static final String MAIN_CLASS = "cc/quarkus/qcc/runtime/main/Main";

    public AddMainClassHook() {
    }

    public void accept(final CompilationContext ctxt) {
        MainMethod mainMethod = MainMethod.get(ctxt);
        String mainClass = mainMethod.getMainClass();
        if (mainClass != null) {
            String mainType = mainClass.replace('.', '/');
            DefinedTypeDefinition definedMainClass = ctxt.getBootstrapClassContext().findDefinedType(mainType);
            if (definedMainClass != null) {
                ValidatedTypeDefinition resolvedMainClass = definedMainClass.validate();
                int idx = resolvedMainClass.findMethodIndex(e -> {
                    // todo: maybe we could simplify this a little...?
                    MethodDescriptor desc = e.getDescriptor();
                    if (desc.getReturnType() != BaseTypeDescriptor.V) {
                        return false;
                    }
                    List<TypeDescriptor> paramTypes = desc.getParameterTypes();
                    if (paramTypes.size() != 1) {
                        return false;
                    }
                    TypeDescriptor paramType = paramTypes.get(0);
                    if (! (paramType instanceof ArrayTypeDescriptor)) {
                        return false;
                    }
                    TypeDescriptor elementType = ((ArrayTypeDescriptor) paramType).getElementTypeDescriptor();
                    if (! (elementType instanceof ClassTypeDescriptor)) {
                        return false;
                    }
                    ClassTypeDescriptor classTypeDescriptor = (ClassTypeDescriptor) elementType;
                    return classTypeDescriptor.getPackageName().equals("java/lang")
                        && classTypeDescriptor.getClassName().equals("String");
                });
                if (idx == -1) {
                    ctxt.error("No valid main method found on \"%s\"", mainClass);
                    return;
                }
                MethodElement mainMethodElement = resolvedMainClass.getMethod(idx);
                if (! mainMethodElement.hasAllModifiersOf(ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC)) {
                    ctxt.error("Main method must be declared public static on \"%s\"", mainClass);
                    return;
                }
                UserMainIntrinsic.register(ctxt, mainMethodElement);
                // now, load and resolve the class with the real entry point on it, causing it to be registered
                DefinedTypeDefinition runtimeMain = ctxt.getBootstrapClassContext().findDefinedType(MAIN_CLASS);
                if (runtimeMain == null) {
                    ctxt.error("Unable to find runtime main class \"%s\"", MAIN_CLASS);
                } else {
                    runtimeMain.validate();
                }
            }
        }
    }
}
