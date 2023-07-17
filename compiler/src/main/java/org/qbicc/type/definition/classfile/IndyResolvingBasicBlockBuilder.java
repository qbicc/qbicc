package org.qbicc.type.definition.classfile;

import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StaticMethodLiteral;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.methodhandle.MethodMethodHandleConstant;

/**
 * A block builder which eagerly resolves dynamic invocations.
 * Note: this will eventually move to the reachability plugin, in order to only bootstrap indys when they can be reached
 * or when they are encountered by the interpreter.
 */
public class IndyResolvingBasicBlockBuilder extends DelegatingBasicBlockBuilder  {
    private static final Logger log = Logger.getLogger("org.qbicc.classfile");

    public IndyResolvingBasicBlockBuilder(final FactoryContext fc, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public Value invokeDynamic(MethodMethodHandleConstant bootstrapHandle, List<Literal> bootstrapArgs, String name, MethodDescriptor descriptor, List<Value> arguments) {
        BasicBlockBuilder gf = getFirstBuilder();
        ClassContext ctxt = getCurrentClassContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        VmThread thread = Vm.requireCurrentThread();
        Vm vm = thread.getVM();
        VmObject methodHandle;
        try {
            // 5.4.3.6, invoking the bootstrap method handle
            // (0.) find the element
            MethodElement targetMethod;
            Value resolvedStaticMethod = gf.resolveStaticMethod(bootstrapHandle.getOwnerDescriptor(), bootstrapHandle.getMethodName(), bootstrapHandle.getDescriptor());
            if (resolvedStaticMethod instanceof StaticMethodLiteral sml) {
                targetMethod = sml.getExecutable();
            } else {
                throw new IllegalStateException();
            }
            // compile the method
            targetMethod.tryCreateMethodBody();
            // 1. allocate the array
            int bootstrapArgCnt = bootstrapArgs.size();
            List<ParameterElement> targetParameters = targetMethod.getParameters();
            TypeParameterContext tpc;
            ExecutableElement currentElement = gf.element();
            if (currentElement instanceof TypeParameterContext) {
                tpc = (TypeParameterContext) currentElement;
            } else {
                tpc = currentElement.getEnclosingType();
            }
            VmClass objectClass = ctxt.findDefinedType("java/lang/Object").load().getVmClass();
            VmReferenceArray args = vm.newArrayOf(objectClass, bootstrapArgCnt);
            VmObject[] argsArray = args.getArray();
            for (int i = 0; i < bootstrapArgCnt; i ++) {
                argsArray[i] = vm.box(ctxt, bootstrapArgs.get(i));
            }
            // 1.5 If the target method is a normal varargs method and the bootstrap args represent
            //     a mix of normal and trailing arguments, bundle the trailing arguments in an array
            //     See JVM Spec 5.4.3.6, task 2, step 2.
            if (bootstrapArgCnt > 0 && targetMethod.isVarargs() && !targetMethod.isSignaturePolymorphic()) {
                int normalStaticArgs = targetParameters.size() - 3 - 1;
                int trailingStaticArgs = bootstrapArgCnt - normalStaticArgs;
                if (trailingStaticArgs > 0 && trailingStaticArgs != bootstrapArgCnt) {
                    TypeDescriptor elementDescriptor = ((ArrayTypeDescriptor) targetParameters.get(targetParameters.size()-1).getTypeDescriptor()).getElementTypeDescriptor();
                    ObjectType elemType = (ObjectType)ctxt.resolveTypeFromDescriptor(elementDescriptor, tpc, TypeSignature.synthesize(ctxt, elementDescriptor));
                    VmObject[] adjustedArgs = Arrays.copyOf(argsArray, normalStaticArgs + 1);
                    VmObject[] trailingArgs = Arrays.copyOfRange(argsArray, normalStaticArgs, argsArray.length);
                    assert normalStaticArgs == adjustedArgs.length - 1;
                    adjustedArgs[normalStaticArgs] = vm.newArrayOf(elemType.getDefinition().load().getVmClass(), trailingArgs);
                    args = vm.newArrayOf(objectClass, adjustedArgs);
                }
            }
            VmReferenceArray appendixResult = vm.newArrayOf(objectClass, 1);
            MethodElement linkCallSite = ctxt.findDefinedType("java/lang/invoke/MethodHandleNatives").load().requireSingleMethod("linkCallSite");
            // 2. call into the VM to link the call site
            final VmClass caller = element().getEnclosingType().load().getVmClass();
            vm.invokeExact(
                linkCallSite,
                null,
                List.of(
                    gf.element().getEnclosingType().load().getVmClass(),
                    Integer.valueOf(-1), // not used
                    vm.createMethodHandle(ctxt, caller, bootstrapHandle),
                    vm.intern(name),
                    vm.createMethodType(ctxt, descriptor),
                    args,
                    appendixResult // <- this is the actual output
                )
            );
            // extract the method handle that we should call through
            methodHandle = appendixResult.getArray()[0];
        } catch (Thrown thrown) {
            log.debug("Failed to create a bootstrap method handle", thrown);
            // Generate code to raise this as a run time error if/when the code is executed
            ClassTypeDescriptor bmeDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/BootstrapMethodError");
            ClassTypeDescriptor thrDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/Throwable");
            Value error = gf.new_(bmeDesc);
            gf.call(gf.resolveConstructor(bmeDesc, MethodDescriptor.synthesize(ctxt, BaseTypeDescriptor.V, List.of(thrDesc))), error, List.of(lf.literalOf(thrown.getThrowable())));
            throw new BlockEarlyTermination(gf.throw_(error));
        }
        // Get the method handle instance from the call site
        ClassTypeDescriptor descOfMethodHandle = ClassTypeDescriptor.synthesize(ctxt, "java/lang/invoke/MethodHandle");
        return gf.call(
            gf.resolveInstanceMethod(descOfMethodHandle, "invokeExact", descriptor),
            lf.literalOf(methodHandle),
            arguments
        );
    }
}
