package org.qbicc.plugin.coreclasses;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

public class RuntimeMethodFinder {
    private static final AttachmentKey<RuntimeMethodFinder> KEY = new AttachmentKey<>();
    private final CompilationContext ctxt;

    final LoadedTypeDefinition VMHelpers;
    final LoadedTypeDefinition ObjectModel;

    private RuntimeMethodFinder(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.VMHelpers = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/VMHelpers").load();
        this.ObjectModel = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/ObjectModel").load();
    }

    public static RuntimeMethodFinder get(CompilationContext ctxt) {
        RuntimeMethodFinder helpers = ctxt.getAttachment(KEY);
        if (helpers == null) {
            helpers = new RuntimeMethodFinder(ctxt);
            RuntimeMethodFinder appearing = ctxt.putAttachmentIfAbsent(KEY, helpers);
            if (appearing != null) {
                helpers = appearing;
            }
        }
        return helpers;
    }

    public MethodElement getMethod(String methodName) {
        int idx = VMHelpers.findMethodIndex(e -> methodName.equals(e.getName()));
        if (idx != -1) {
            return VMHelpers.getMethod(idx);
        }
        idx = ObjectModel.findMethodIndex(e -> methodName.equals(e.getName()));
        if (idx != -1) {
            return ObjectModel.getMethod(idx);
        }
        ctxt.error("Can't find the runtime helper method %s", methodName);
        return null;
    }

    public MethodElement getMethod(String runtimeClass, String helperName) {
        ClassContext context = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition dtd = context.findDefinedType(runtimeClass);
        if (dtd == null) {
            ctxt.error("Can't find runtime library class: " + runtimeClass);
            return null;
        }
        LoadedTypeDefinition ltd = dtd.load();
        int idx = ltd.findMethodIndex(e -> helperName.equals(e.getName()));
        if (idx == -1) {
            ctxt.error("Can't find the runtime helper method %s", helperName);
            return null;
        }
        return ltd.getMethod(idx);
    }

    public ConstructorElement getConstructor(String runtimeClass, MethodDescriptor descriptor) {
        ClassContext context = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition dtd = context.findDefinedType(runtimeClass);
        if (dtd == null) {
            ctxt.error("Can't find runtime library class: " + runtimeClass);
            return null;
        }
        LoadedTypeDefinition ltd = dtd.load();
        int idx = ltd.findConstructorIndex(descriptor);
        if (idx == -1) {
            ctxt.error("Can't find the constructor with descriptor %s for class %s", descriptor.toString(), runtimeClass);
        }
        return ltd.getConstructor(idx);
    }
}
