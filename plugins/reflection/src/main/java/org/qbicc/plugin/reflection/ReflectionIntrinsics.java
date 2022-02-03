package org.qbicc.plugin.reflection;


import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.methodhandle.MethodHandleKind;

final class ReflectionIntrinsics {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.reflection");

    private ReflectionIntrinsics() {}

    static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();

        ClassTypeDescriptor methodHandleDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodHandle");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor methodTypeDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodType");
        ClassTypeDescriptor internalErrorDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/InternalError");
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");

        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);

        MethodDescriptor objArrayToObj = MethodDescriptor.synthesize(classContext, objDesc, List.of(objArrayDesc));
        MethodDescriptor throwableToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(throwableDesc));

        LoadedTypeDefinition dmhDef = classContext.findDefinedType("java/lang/invoke/DirectMethodHandle").load();
        FieldElement dmhMember = dmhDef.findField("member");
        LoadedTypeDefinition dmhCtorDef = classContext.findDefinedType("java/lang/invoke/DirectMethodHandle$Constructor").load();
        FieldElement dmhCtorInitMethod = dmhCtorDef.findField("initMethod");
        FieldElement dmhCtorInstanceClass = dmhCtorDef.findField("instanceClass");

        // mh.invoke(...) -> mh.asType(actualType).invokeExact(...)
        InstanceIntrinsic invoke = (builder, instance, target, arguments) -> {
            // Use first builder because we chain to other intrinsics!
            MethodDescriptor descriptor = target.getCallSiteDescriptor();
            Vm vm = Vm.requireCurrent();
            BasicBlockBuilder fb = builder.getFirstBuilder();
            try {
                VmObject realType = vm.createMethodType(classContext, descriptor);
                Value realHandle;
                LoadedTypeDefinition mhDef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/invoke/MethodHandle").load();
                int asTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("asType"));
                MethodElement asType = mhDef.getMethod(asTypeIdx);
                if (instance instanceof ObjectLiteral) {
                    // get the target statically
                    realHandle = lf.literalOf((VmObject) vm.invokeExact(asType, ((ObjectLiteral) instance).getValue(), List.of(realType)));
                } else {
                    // get the target dynamically
                    ValueHandle asTypeHandle = fb.exactMethodOf(instance, asType);
                    realHandle = fb.call(asTypeHandle, List.of(lf.literalOf(realType)));
                }
                ValueHandle invokeExactHandle = fb.exactMethodOf(realHandle, methodHandleDesc, "invokeExact", descriptor);
                return fb.call(invokeExactHandle, arguments);
            } catch (Thrown t) {
                ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invoke intrinsic: %s", t);
                log.warnf(t, "Failed to expand MethodHandle.invoke intrinsic");
                Value ie = fb.new_(internalErrorDesc);
                fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                throw new BlockEarlyTermination(fb.throw_(ie));
            }
        };
        // this intrinsic MUST be added during ADD because `invoke` must always be converted.
        intrinsics.registerIntrinsic(Phase.ADD, methodHandleDesc, "invoke", objArrayToObj, invoke);

        // The actual method handle dispatcher
        InstanceIntrinsic invokeExact = (builder, instance, target, arguments) -> {
            Reflection reflection = Reflection.get(ctxt);
            BasicBlockBuilder fb = builder.getFirstBuilder();
            if (instance instanceof ObjectLiteral mhLit) {
                InstanceMethodType callSiteType = target.getCallSiteType();
                MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
                // replace with the target invocation type
                Vm vm = Vm.requireCurrent();
                LoadedTypeDefinition callerTypeDef = fb.getCurrentElement().getEnclosingType().load();
                VmClass callerClass = callerTypeDef.getVmClass();
                int refKind = MethodHandleKind.INVOKE_VIRTUAL.getId();
                LoadedTypeDefinition mhDef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/invoke/MethodHandle").load();
                VmClass defClass = mhDef.getVmClass();
                VmString name = vm.intern("invoke");
                // MethodType from java.lang.invoke.MethodHandleNatives.findMethodHandleType
                VmClass objClass = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getVmClass();
                VmClass classClass = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().getVmClass();
                VmClassLoader cl = vm.getClassLoaderForContext(callerTypeDef.getContext());
                VmClass retType = vm.getClassForDescriptor(cl, callSiteDescriptor.getReturnType());
                List<TypeDescriptor> parameterDescs = callSiteDescriptor.getParameterTypes();
                int paramCnt = parameterDescs.size();
                VmReferenceArray pArray = vm.newArrayOf(classClass, paramCnt);
                for (int i = 0; i < paramCnt; i ++) {
                    pArray.store(i, vm.getClassForDescriptor(cl, parameterDescs.get(i)));
                }
                VmObject type = (VmObject) vm.invokeExact(reflection.methodHandleNativesFindMethodHandleType, null, List.of(
                    retType,
                    pArray
                ));
                // holds the *returned* appendix object
                VmReferenceArray appendixResult = vm.newArrayOf(objClass, 1);
                VmObject invokerMemberName = (VmObject) vm.invokeExact(reflection.methodHandleNativesLinkMethod, null, List.of(
                    callerClass,
                    Integer.valueOf(refKind),
                    defClass,
                    name,
                    type,
                    appendixResult
                ));
                // resolve it
                vm.invokeExact(reflection.methodHandleNativesResolve, null, List.of(invokerMemberName));
                int methodIdx = invokerMemberName.getMemory().load32(invokerMemberName.indexOf(reflection.memberNameIndexField), SinglePlain);
                VmClass methodClazz = (VmClass) invokerMemberName.getMemory().loadRef(invokerMemberName.indexOf(reflection.memberNameClazzField), SinglePlain);
                MethodElement method = methodClazz.getTypeDefinition().getMethod(methodIdx);
                // the invoker will be static
                List<Value> args = new ArrayList<>();
                // add the method handle
                args.add(mhLit);
                // add arguments
                args.addAll(arguments);
                // add the type (optionally)
                if (method.getType().getParameterCount() > args.size()) {
                    args.add(lf.literalOf(type));
                }
                return fb.call(fb.staticMethod(method), args);
            } else {
                ctxt.warning(fb.getLocation(), "Non-constant method handles not yet supported");
                Value ie = fb.new_(internalErrorDesc);
                fb.call(fb.constructorOf(ie, internalErrorDesc, MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
                throw new BlockEarlyTermination(fb.throw_(ie));
            }
        };

        intrinsics.registerIntrinsic(Phase.ADD, methodHandleDesc, "invokeExact", objArrayToObj, invokeExact);

        InstanceIntrinsic invokeBasic = (builder, instance, target, arguments) -> {
            ClassTypeDescriptor mhDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MethodHandle");
            ClassTypeDescriptor mnDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/MemberName");
            ClassTypeDescriptor lfDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/invoke/LambdaForm");

            BasicBlockBuilder fb = builder.getFirstBuilder();
            // we want to extract the lambda form and invoke on that...
            Value form = fb.load(fb.instanceFieldOf(fb.referenceHandle(instance), mhDesc, "form", lfDesc));
            Value memberName = fb.load(fb.instanceFieldOf(fb.referenceHandle(form), lfDesc, "vmentry", mnDesc));
            // lambda forms are static, so get the static method pointer off of the vmentry
            Value smPtr = /* TODO */ lf.zeroInitializerLiteralOfType(ts.getVoidType().getPointer());
            // cast it to the call site type, which *must* match
            InstanceMethodType callSiteType = target.getCallSiteType();
            StaticMethodType invokeType = ts.getStaticMethodType(callSiteType.getReturnType(), callSiteType.getParameterTypes());
            Value castPtr = fb.bitCast(smPtr, invokeType.getPointer());
            // unlike linkTo..., do not replace the call frame (todo: maybe we should though)
            return fb.call(fb.pointerHandle(castPtr), arguments);
        };

        intrinsics.registerIntrinsic(Phase.ADD, methodHandleDesc, "invokeBasic", objArrayToObj, invokeBasic);

        InstanceIntrinsic linkToStatic = (builder, instance, target, arguments) -> {
            BasicBlockBuilder fb = builder.getFirstBuilder();
            // the last argument is the MemberName
            int argSize = arguments.size();
            Value memberName = arguments.get(argSize - 1);
            // get the target pointer
            Value smPtr = /* TODO */ lf.zeroInitializerLiteralOfType(ts.getVoidType().getPointer());
            // cast it to the call site type, which *must* match
            InstanceMethodType callSiteType = target.getCallSiteType();
            StaticMethodType invokeType = ts.getStaticMethodType(callSiteType.getReturnType(), callSiteType.getParameterTypes().subList(0, argSize - 1));
            Value castPtr = fb.bitCast(smPtr, invokeType.getPointer());
            // tail call to target
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castPtr), arguments.subList(0, argSize - 1)));
        };

        intrinsics.registerIntrinsic(Phase.ADD, methodHandleDesc, "linkToStatic", objArrayToObj, linkToStatic);

        // TODO:
        //    static native @PolymorphicSignature Object linkToVirtual(Object... args) throws Throwable;
        //    static native @PolymorphicSignature Object linkToSpecial(Object... args) throws Throwable;
        //    static native @PolymorphicSignature Object linkToInterface(Object... args) throws Throwable;
    }
}
