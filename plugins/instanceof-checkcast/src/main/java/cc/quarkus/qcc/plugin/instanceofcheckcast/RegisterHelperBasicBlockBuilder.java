package cc.quarkus.qcc.plugin.instanceofcheckcast;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A BasicBlockBuilder which registers the necessary helper calls with the QCC compiler.
 */
public class RegisterHelperBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    ValidatedTypeDefinition vmHelpersVTD;

    public RegisterHelperBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        vmHelpersVTD = getVMHelpersVTD(ctxt);
    }

    private static ValidatedTypeDefinition getVMHelpersVTD(final CompilationContext ctxt) {
        ValidatedTypeDefinition vtd = null;
        if (!InstanceOfCheckCastBasicBlockBuilder.PLUGIN_DISABLED) {
            ClassContext bootstrapCC = ctxt.getBootstrapClassContext();
            DefinedTypeDefinition dtd = bootstrapCC.findDefinedType("cc/quarkus/qcc/runtime/main/VMHelpers");
            if (dtd != null) {
                vtd = dtd.validate();
            }
        }
        return vtd;
    }

    public Value instanceOf(final Value input, ObjectType classFileType, final ValueType expectedType) {
        if (!InstanceOfCheckCastBasicBlockBuilder.PLUGIN_DISABLED) {
            // Only force loading if the plugin is enabled
            MethodElement methodElement = ctxt.getVMHelperMethod("fast_instanceof");
            ctxt.registerEntryPoint(methodElement);
            ctxt.enqueue(methodElement);
        }
        return super.instanceOf(input, classFileType, expectedType);
    }

    public Value classOf(final Value instance) {
        MethodElement methodElement = ctxt.getVMHelperMethod("classof_from_typeid");
        ctxt.registerEntryPoint(methodElement);
        ctxt.enqueue(methodElement);
        return super.classOf(instance);
    }
}
