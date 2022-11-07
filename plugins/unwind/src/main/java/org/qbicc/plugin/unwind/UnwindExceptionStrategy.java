package org.qbicc.plugin.unwind;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ClassContext;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

import java.util.List;

public class UnwindExceptionStrategy {
    private static final AttachmentKey<UnwindExceptionStrategy> KEY = new AttachmentKey<>();

    private final FieldElement unwindExceptionField;
    private final MethodElement raiseExceptionMethod;
    private final MethodElement personalityMethod;

    private UnwindExceptionStrategy(final CompilationContext ctxt) {
        /* Inject a field "unwindException" of type Unwind$_Unwind_Exception in j.l.Thread */
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/unwind/Unwind$struct__Unwind_Exception");
        DefinedTypeDefinition threadDef = classContext.findDefinedType("java/lang/Thread");
        unwindExceptionField = threadDef.load().resolveField(desc, "unwindException", true);

        /* Get the symbol to Unwind#_Unwind_RaiseException */
        String unwindClass = "org/qbicc/runtime/unwind/Unwind";
        DefinedTypeDefinition unwindDefined = classContext.findDefinedType(unwindClass);
        if (unwindDefined != null) {
            LoadedTypeDefinition unwindValidated = unwindDefined.load();
            int index = unwindValidated.findMethodIndex(e -> e.getName().equals("_Unwind_RaiseException"));
            raiseExceptionMethod = unwindValidated.getMethod(index);
            index = unwindValidated.findMethodIndex(e -> e.getName().equals("personality"));
            personalityMethod = unwindValidated.getMethod(index);
        } else {
            raiseExceptionMethod = null;
            personalityMethod = null;
            ctxt.error("Required class \"%s\" is not found on boot classpath", unwindClass);
        }
    }

    public static UnwindExceptionStrategy get(CompilationContext ctxt) {
        UnwindExceptionStrategy helper = ctxt.getAttachment(KEY);
        if (helper == null) {
            helper = new UnwindExceptionStrategy(ctxt);
            UnwindExceptionStrategy appearing = ctxt.putAttachmentIfAbsent(KEY, helper);
            if (appearing != null) {
                helper = appearing;
            }
        }
        return helper;
    }

    public static void init(CompilationContext ctxt) {
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/unwind/Unwind$struct__Unwind_Exception");
        ClassTypeDescriptor serAsZero = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/SerializeAsZero");
        Patcher.get(ctxt).addField(classContext, "java/lang/Thread", "unwindException", desc, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setSignature(TypeSignature.synthesize(classContext, desc));
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_RESOLVE | ClassFile.I_ACC_NO_REFLECT);
                builder.addInvisibleAnnotations(List.of(Annotation.synthesize(serAsZero)));
                DefinedTypeDefinition jltDefined = classContext.findDefinedType("java/lang/Thread");
                builder.setEnclosingType(jltDefined);
                return builder.build();
            }
        }, 0, 0);
    }

    public FieldElement getUnwindExceptionField() {
        return this.unwindExceptionField;
    }

    public MethodElement getRaiseExceptionMethod() {
        return this.raiseExceptionMethod;
    }

    public MethodElement getPersonalityMethod() {
        return personalityMethod;
    }
}
