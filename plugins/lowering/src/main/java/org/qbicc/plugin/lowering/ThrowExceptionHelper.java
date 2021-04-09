package org.qbicc.plugin.lowering;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.ValidatedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

public class ThrowExceptionHelper {
    private static final AttachmentKey<ThrowExceptionHelper> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final FieldElement unwindExceptionField;
    private final MethodElement raiseExceptionMethod;

    private ThrowExceptionHelper(final CompilationContext ctxt) {
        this.ctxt = ctxt;

        /* Inject a field "unwindException" of type Unwind$_Unwind_Exception in j.l.Thread */
        FieldElement.Builder builder = FieldElement.builder();
        builder.setName("unwindException");
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/unwind/Unwind$struct__Unwind_Exception");
        builder.setDescriptor(desc);
        builder.setSignature(TypeSignature.synthesize(classContext, desc));
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
        DefinedTypeDefinition jltDefined = classContext.findDefinedType("java/lang/Thread");
        builder.setEnclosingType(jltDefined);
        FieldElement field = builder.build();
        jltDefined.validate().injectField(field);
        unwindExceptionField = field;

        /* Get the symbol to Unwind#_Unwind_RaiseException */
        String unwindClass = "org/qbicc/runtime/unwind/Unwind";
        DefinedTypeDefinition unwindDefined = classContext.findDefinedType(unwindClass);
        if (unwindDefined != null) {
            ValidatedTypeDefinition unwindValidated = unwindDefined.validate();
            int index = unwindValidated.findMethodIndex(e -> e.getName().equals("_Unwind_RaiseException"));
            raiseExceptionMethod = unwindValidated.getMethod(index);
        } else {
            raiseExceptionMethod = null;
            ctxt.error("Required class \"%s\" is not found on boot classpath", unwindClass);
        }
    }

    public static ThrowExceptionHelper get(CompilationContext ctxt) {
        ThrowExceptionHelper helper = ctxt.getAttachment(KEY);
        if (helper == null) {
            helper = new ThrowExceptionHelper(ctxt);
            ThrowExceptionHelper appearing = ctxt.putAttachmentIfAbsent(KEY, helper);
            if (appearing != null) {
                helper = appearing;
            }
        }
        return helper;
    }

    public FieldElement getUnwindExceptionField() {
        return this.unwindExceptionField;
    }

    public MethodElement getRaiseExceptionMethod() {
        return this.raiseExceptionMethod;
    }
}
