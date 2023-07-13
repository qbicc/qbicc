package org.qbicc.plugin.unwind;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.StructType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

public class UnwindExceptionStrategy {
    private static final AttachmentKey<UnwindExceptionStrategy> KEY = new AttachmentKey<>();

    private final StructType.Member unwindExceptionMember;
    private final MethodElement raiseExceptionMethod;
    private final MethodElement personalityMethod;

    private UnwindExceptionStrategy(final CompilationContext ctxt) {
        /* Locate the field "unwindException" of type Unwind$_Unwind_Exception in thread_native */
        ClassContext classContext = ctxt.getBootstrapClassContext();
        StructType st = (StructType) classContext.resolveTypeFromClassName("jdk/internal/thread", "ThreadNative$thread_native");
        unwindExceptionMember = st.getMember("unwindException");

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
    }

    public StructType.Member getUnwindExceptionMember() {
        return this.unwindExceptionMember;
    }

    public MethodElement getRaiseExceptionMethod() {
        return this.raiseExceptionMethod;
    }

    public MethodElement getPersonalityMethod() {
        return personalityMethod;
    }
}
