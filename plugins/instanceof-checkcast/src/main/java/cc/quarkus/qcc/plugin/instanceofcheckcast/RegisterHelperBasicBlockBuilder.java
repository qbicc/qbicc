package cc.quarkus.qcc.plugin.instanceofcheckcast;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
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
    }

    private ValidatedTypeDefinition getVMHelpersVTD() {
        ValidatedTypeDefinition vtd = vmHelpersVTD;
        if (vtd == null) {
            ClassContext bootstrapCC = ctxt.getBootstrapClassContext();
            DefinedTypeDefinition dtd = bootstrapCC.findDefinedType("cc/quarkus/qcc/runtime/main/VMHelpers");
            if (dtd != null) {
                vtd = dtd.validate();
                vmHelpersVTD = vtd; // racy write - OK as all VTDs for the same class are equivalent
            }
        }
        return vtd;
    }

    public Value instanceOf(final Value input, final ValueType expectedType) {
        ValidatedTypeDefinition resolved = getVMHelpersVTD();
        int idx = resolved.findMethodIndex(e -> "fast_instanceof".equals(e.getName()));
        assert(idx != -1);
        MethodElement methodElement = resolved.getMethod(idx);
        ctxt.registerEntryPoint(methodElement);
        return super.instanceOf(input, expectedType);
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        return super.instanceOf(input, desc);
    }
}
