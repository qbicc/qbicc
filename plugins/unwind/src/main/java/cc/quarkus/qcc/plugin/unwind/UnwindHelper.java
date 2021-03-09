package cc.quarkus.qcc.plugin.unwind;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;

public class UnwindHelper {
    static final AttachmentKey<UnwindHelper> KEY = new AttachmentKey<>();
    private final MethodElement personalityMethod;

    private UnwindHelper(final CompilationContext ctxt) {
        String unwindClass = "cc/quarkus/qcc/runtime/unwind/Unwind";
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition unwindDefined = classContext.findDefinedType(unwindClass);
        if (unwindDefined != null) {
            ValidatedTypeDefinition unwindValidated = unwindDefined.validate();
            int index = unwindValidated.findMethodIndex(e -> e.getName().equals("personality"));
            personalityMethod = unwindValidated.getMethod(index);
        } else {
            personalityMethod = null;
            ctxt.error("Required class \"%s\" is not found on boot classpath", unwindClass);
        }
    }
    public static UnwindHelper get(CompilationContext ctxt) {
        UnwindHelper helper = ctxt.getAttachment(KEY);
        if (helper == null) {
            helper = new UnwindHelper(ctxt);
            UnwindHelper appearing = ctxt.putAttachmentIfAbsent(KEY, helper);
            if (appearing != null) {
                helper = appearing;
            }
        }
        return helper;
    }

    public MethodElement getPersonalityMethod() {
        return personalityMethod;
    }
}
