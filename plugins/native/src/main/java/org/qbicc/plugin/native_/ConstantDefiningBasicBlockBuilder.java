package org.qbicc.plugin.native_;

import java.io.IOException;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.driver.Driver;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.StaticInvocationValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.runtime.CNative;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * This block builder replaces calls to the {@link CNative#constant()} method with a registration of the constant
 * field value.
 */
public class ConstantDefiningBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ConstantDefiningBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        ExecutableElement element = getCurrentElement();
        if (element instanceof InitializerElement) {
            NativeInfo.get(ctxt).registerInitializer((InitializerElement) element);
        }
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        if (handle instanceof StaticField) {
            FieldElement fieldElement = ((StaticField) handle).getVariableElement();
            if (fieldElement.isReallyFinal()) {
                // initialize the constant if any
                InitializerElement initializerElement = fieldElement.getEnclosingType().load().getInitializer();
                if (NativeInfo.get(ctxt).registerInitializer(initializerElement)) {
                    if (initializerElement.hasMethodBody()) {
                        initializerElement.getOrCreateMethodBody();
                    }
                }
            }
        }
        return super.load(handle, mode);
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        Value test = value;
        while (test instanceof CastValue) {
            test = ((CastValue) test).getInput();
        }
        if (test instanceof StaticInvocationValue) {
            StaticInvocationValue inv = (StaticInvocationValue) test;
            MethodElement invocationTarget = inv.getInvocationTarget();
            if (invocationTarget.getEnclosingType().internalPackageAndNameEquals(Native.NATIVE_PKG, Native.C_NATIVE)) {
                if (invocationTarget.getName().equals("constant")) {
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
            }
        }
        return super.store(handle, value, mode);
    }

    private void processConstant(final FieldElement fieldElement) {
        CProbe.Builder builder = CProbe.builder();
        // get the element's info
        String name = fieldElement.getName();
        for (Annotation annotation : fieldElement.getVisibleAnnotations()) {
            ClassTypeDescriptor desc = annotation.getDescriptor();
            if (ProbeUtils.processCommonAnnotation(builder, annotation)) {
                continue;
            }
            if (desc.getPackageName().equals(Native.NATIVE_PKG) && desc.getClassName().equals(Native.ANN_NAME)) {
                name = ((StringAnnotationValue) annotation.getValue("value")).getString();
            }
        }
        for (Annotation annotation : fieldElement.getEnclosingType().getVisibleAnnotations()) {
            ProbeUtils.processCommonAnnotation(builder, annotation);
        }
        // todo: recursively process enclosing types (requires InnerClasses support)
        Location location = getLocation();
        builder.probeConstant(name, location.getSourceFilePath(), location.getLineNumber());
        // run the probe
        CProbe probe = builder.build();
        Constants constants = Constants.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();
        CProbe.Result result;
        try {
            result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), null);
            if (result == null) {
                // constant is undefined
                constants.registerConstant(fieldElement, lf.literalOfUndefined());
                return;
            }
        } catch (IOException e) {
            // constant is undefined either way
            constants.registerConstant(fieldElement, lf.literalOfUndefined());
            return;
        }
        CProbe.ConstantInfo constantInfo = result.getConstantInfo(name);
        // compute the type and raw value
        ValueType type = fieldElement.getType();
        Value val;
        // todo: if constant value is actually a symbol ref...
        if (type instanceof IntegerType) {
            val = lf.literalOf(constantInfo.getValueAsInt());
        } else if (type instanceof FloatType) {
            val = lf.literalOf(Float.intBitsToFloat(constantInfo.getValueAsInt()));
        } else {
            val = lf.literalOfUndefined();
        }
        if (constantInfo.isDefined()) {
            val = lf.literalOfDefinedConstant(name, val);
        }
        constants.registerConstant(fieldElement, val);
    }
}
