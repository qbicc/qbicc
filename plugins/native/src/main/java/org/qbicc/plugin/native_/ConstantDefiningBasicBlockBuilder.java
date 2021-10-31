package org.qbicc.plugin.native_;

import java.io.IOException;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.driver.Driver;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.runtime.CNative;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * This block builder replaces calls to the {@link CNative#constant()} method with a registration of the constant
 * field value.
 */
public class ConstantDefiningBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    private ConstantDefiningBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        ExecutableElement element = getCurrentElement();
        if (element instanceof InitializerElement) {
            NativeInfo.get(ctxt).registerInitializer((InitializerElement) element);
        }
        this.ctxt = ctxt;
    }

    public static BasicBlockBuilder createIfNeeded(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        if (delegate.getCurrentElement() instanceof InitializerElement) {
            return new ConstantDefiningBasicBlockBuilder(ctxt, delegate);
        } else {
            return delegate;
        }
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        Value test = value;
        while (test instanceof CastValue) {
            test = ((CastValue) test).getInput();
        }
        if (test instanceof ConstantLiteral) {
            // it's a constant
            if (handle instanceof StaticField) {
                FieldElement fieldElement = ((StaticField) handle).getVariableElement();
                if (fieldElement.isReallyFinal()) {
                    processConstant(fieldElement);
                    // don't actually write the field at all
                    return nop();
                }
            }
            ctxt.error(getLocation(), "Compilation constants must be static final fields");
            return nop();
        }
        return super.store(handle, value, mode);
    }

    private void processConstant(final FieldElement fieldElement) {
        Constants constants = Constants.get(ctxt);
        ClassContext classContext = fieldElement.getEnclosingType().getContext();
        /* Capture location during the ADD phase since constants are defined lazily. */
        Location location = getLocation();
        constants.registerConstant(fieldElement, () -> {
            CProbe.Builder builder = CProbe.builder();
            // get the element's info
            String name = fieldElement.getName();
            // process enclosing type first
            ProbeUtils.ProbeProcessor pp = new ProbeUtils.ProbeProcessor(classContext, fieldElement.getEnclosingType());
            for (Annotation annotation : fieldElement.getEnclosingType().getInvisibleAnnotations()) {
                pp.processAnnotation(annotation);
            }
            pp.accept(builder);
            // now process the annotated member so it can override
            pp = new ProbeUtils.ProbeProcessor(classContext, fieldElement);
            for (Annotation annotation : fieldElement.getInvisibleAnnotations()) {
                ClassTypeDescriptor desc = annotation.getDescriptor();
                if (pp.processAnnotation(annotation)) {
                    continue;
                }
                if (desc.getPackageName().equals(Native.NATIVE_PKG) && desc.getClassName().equals(Native.ANN_NAME)) {
                    name = ((StringAnnotationValue) annotation.getValue("value")).getString();
                }
            }
            pp.accept(builder);
            // todo: recursively process enclosing types (requires InnerClasses support)
            builder.probeConstant(name, location.getSourceFilePath(), location.getLineNumber());
            // run the probe
            CProbe probe = builder.build();
            LiteralFactory lf = ctxt.getLiteralFactory();
            CProbe.Result result;
            try {
                result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), null);
                if (result == null) {
                    // constant is undefined
                    return lf.undefinedLiteralOfType(fieldElement.getType());
                }
            } catch (IOException e) {
                // constant is undefined either way
                return lf.undefinedLiteralOfType(fieldElement.getType());
            }
            CProbe.ConstantInfo constantInfo = result.getConstantInfo(name);
            // compute the type and raw value
            return constantInfo.getValueAsLiteralOfType(ctxt.getTypeSystem(), lf, fieldElement.getType());
        });
    }
}
