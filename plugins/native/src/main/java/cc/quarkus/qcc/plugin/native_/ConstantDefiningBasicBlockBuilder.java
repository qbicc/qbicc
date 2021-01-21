package cc.quarkus.qcc.plugin.native_;

import java.io.IOException;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Narrow;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.StaticInvocationValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.machine.probe.CProbe;
import cc.quarkus.qcc.plugin.constants.Constants;
import cc.quarkus.qcc.runtime.CNative;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;

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

    public Value readStaticField(final FieldElement fieldElement, final JavaAccessMode mode) {
        if (fieldElement.hasAllModifiersOf(ClassFile.ACC_STATIC | ClassFile.ACC_FINAL)) {
            // initialize the constant if any
            InitializerElement initializerElement = fieldElement.getEnclosingType().validate().getInitializer();
            if (NativeInfo.get(ctxt).registerInitializer(initializerElement)) {
                if (initializerElement.hasMethodBody()) {
                    initializerElement.getOrCreateMethodBody();
                }
            }
        }
        return super.readStaticField(fieldElement, mode);
    }

    public Node writeStaticField(final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        Value test = value;
        while (test instanceof Narrow) {
            test = ((Narrow) test).getInput();
        }
        if (test instanceof StaticInvocationValue) {
            StaticInvocationValue inv = (StaticInvocationValue) test;
            MethodElement invocationTarget = inv.getInvocationTarget();
            if (invocationTarget.getEnclosingType().internalPackageAndNameEquals(Native.NATIVE_PKG, Native.C_NATIVE)) {
                if (invocationTarget.getName().equals("constant")) {
                    // it's a constant
                    if (! fieldElement.hasAllModifiersOf(ClassFile.ACC_STATIC | ClassFile.ACC_FINAL)) {
                        ctxt.error(getLocation(), "Compilation constants must be static and final");
                        return nop();
                    }
                    processConstant(fieldElement);
                    // don't actually write the field at all
                    return nop();
                }
            }
        }
        return getDelegate().writeStaticField(fieldElement, value, mode);
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
        ValueType type = fieldElement.getType(List.of(/*todo*/));
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
