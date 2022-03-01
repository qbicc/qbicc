package org.qbicc.plugin.reflection;


import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.InstanceMethodElementHandle;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.pointer.InstanceMethodPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

public final class ReflectionIntrinsics {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.reflection");

    private ReflectionIntrinsics() {}

    public static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        Patcher patcher = Patcher.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        String methodHandleInt = "java/lang/invoke/MethodHandle";
        ClassTypeDescriptor methodHandleDesc = ClassTypeDescriptor.synthesize(classContext, methodHandleInt);
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor internalErrorDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/InternalError");
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");

        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);

        MethodDescriptor objArrayToObj = MethodDescriptor.synthesize(classContext, objDesc, List.of(objArrayDesc));
        MethodDescriptor throwableToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(throwableDesc));


        // mh.invoke(...) → mh.asType(actualType).invokeExact(...)

        InstanceIntrinsic invokeIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (instance instanceof ObjectLiteral instanceLit) {
                // perform a build-time check
                MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
                BasicBlockBuilder fb = builder.getFirstBuilder();
                Vm vm = Vm.requireCurrent();
                try {
                    VmObject realType = vm.createMethodType(classContext, callSiteDescriptor);
                    Value realHandle;
                    LoadedTypeDefinition mhDef = target.getExecutable().getEnclosingType().load();
                    int asTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("asType"));
                    MethodElement asType = mhDef.getMethod(asTypeIdx);
                    // get the target statically
                    realHandle = lf.literalOf((VmObject) vm.invokeVirtual(asType, instanceLit.getValue(), List.of(realType)));
                    // and transform to `invokeExact`
                    ValueHandle invokeExactHandle = fb.exactMethodOf(realHandle, methodHandleDesc, "invokeExact", callSiteDescriptor);
                    return fb.call(invokeExactHandle, arguments);
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invoke intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.invoke intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "invoke", invokeIntrinsic);

        // (non-intrinsic version)
        InstanceIntrinsic invokeMethodBody = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Vm vm = Vm.requireCurrent();
            try {
                VmObject realType = vm.createMethodType(classContext, callSiteDescriptor);
                Value realHandle;
                LoadedTypeDefinition mhDef = target.getExecutable().getEnclosingType().load();
                int asTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("asType"));
                MethodElement asType = mhDef.getMethod(asTypeIdx);
                // get the target dynamically
                ValueHandle asTypeHandle = fb.virtualMethodOf(instance, asType);
                realHandle = fb.call(asTypeHandle, List.of(lf.literalOf(realType)));
                // and transform to `invokeExact`
                ValueHandle invokeExactHandle = fb.exactMethodOf(realHandle, methodHandleDesc, "invokeExact", callSiteDescriptor);
                throw new BlockEarlyTermination(fb.tailCall(invokeExactHandle, arguments));
            } catch (Thrown t) {
                ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invoke method: %s", t);
                log.warnf(t, "Failed to expand MethodHandle.invoke method");
                Value ie = fb.new_(internalErrorDesc);
                fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                throw new BlockEarlyTermination(fb.throw_(ie));
            }
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "invoke", objArrayToObj, new InstanceIntrinsicMethodBodyFactory(invokeMethodBody), 0);

        // mh.invokeExact(...) → mh.checkType(type); mh.invokeBasic(...)

        InstanceIntrinsic invokeExactIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (instance instanceof ObjectLiteral instanceLit) {
                MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
                BasicBlockBuilder fb = builder.getFirstBuilder();
                Vm vm = Vm.requireCurrent();
                try {
                    VmObject realType = vm.createMethodType(classContext, callSiteDescriptor);
                    LoadedTypeDefinition mhDef = target.getExecutable().getEnclosingType().load();
                    int checkTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("checkType"));
                    MethodElement checkType = mhDef.getMethod(checkTypeIdx);
                    // check the type now
                    vm.invokeExact(checkType, instanceLit.getValue(), List.of(realType));
                    // type is OK; now we can forward to invokeBasic(...) on this same instance
                    ValueHandle invokeBasicHandle = fb.exactMethodOf(instance, methodHandleDesc, "invokeBasic", callSiteDescriptor);
                    return fb.call(invokeBasicHandle, arguments);
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invokeExact intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.invokeExact intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "invokeExact", invokeExactIntrinsic);

        InstanceIntrinsic invokeExactMethodBody = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Vm vm = Vm.requireCurrent();
            try {
                VmObject realType = vm.createMethodType(classContext, callSiteDescriptor);
                LoadedTypeDefinition mhDef = target.getExecutable().getEnclosingType().load();
                int checkTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("checkType"));
                MethodElement checkType = mhDef.getMethod(checkTypeIdx);
                ValueHandle checkTypeHandle = fb.exactMethodOf(instance, checkType);
                // check the type dynamically at run time
                fb.call(checkTypeHandle, List.of(lf.literalOf(realType)));
                // and transform to `invokeBasic`
                ValueHandle invokeBasicHandle = fb.exactMethodOf(instance, methodHandleDesc, "invokeBasic", callSiteDescriptor);
                throw new BlockEarlyTermination(fb.tailCall(invokeBasicHandle, arguments));
            } catch (Thrown t) {
                ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invokeExact method: %s", t);
                log.warnf(t, "Failed to expand MethodHandle.invokeExact method");
                Value ie = fb.new_(internalErrorDesc);
                fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                throw new BlockEarlyTermination(fb.throw_(ie));
            }
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "invokeExact", objArrayToObj, new InstanceIntrinsicMethodBodyFactory(invokeExactMethodBody), 0);

        // mh.invokeBasic(...) → (*mh.form.vmentry)(...)

        InstanceIntrinsic invokeBasicIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (instance instanceof ObjectLiteral instanceLit) {
                Reflection reflection = Reflection.get(ctxt);
                BasicBlockBuilder fb = builder.getFirstBuilder();
                try {
                    VmObject methodHandle = instanceLit.getValue();
                    VmObject lambdaForm = methodHandle.getMemory().loadRef(methodHandle.indexOf(reflection.methodHandleLambdaFormField), SinglePlain);
                    VmObject memberName = lambdaForm.getMemory().loadRef(lambdaForm.indexOf(reflection.lambdaFormMemberNameField), SinglePlain);
                    Pointer methodPtr = memberName.getMemory().loadPointer(memberName.indexOf(reflection.memberNameExactDispatcherField), SinglePlain);
                    // pass the method handle to the lambda form
                    ArrayList<Value> newArgs = new ArrayList<>(arguments.size() + 1);
                    newArgs.add(instance);
                    newArgs.addAll(arguments);
                    if (methodPtr instanceof StaticMethodPointer smp) {
                        // call the static method directly
                        return fb.call(fb.staticMethod(smp.getStaticMethod()), newArgs);
                    } else if (methodPtr instanceof InstanceMethodPointer imp) {
                        // todo: for now, we're using static helpers only
                        // call the exact method directly
                        return fb.call(fb.exactMethodOf(arguments.get(0), imp.getInstanceMethod()), newArgs.subList(1, newArgs.size()));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invokeBasic intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.invokeBasic intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "invokeBasic", invokeBasicIntrinsic);

        InstanceIntrinsic invokeBasicMethodBody = (builder, instance, target, arguments) -> {
            TypeSystem ts = ctxt.getTypeSystem();
            Reflection reflection = Reflection.get(ctxt);
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Value lambdaForm = fb.load(fb.instanceFieldOf(fb.referenceHandle(instance), reflection.methodHandleLambdaFormField));
            Value memberName = fb.load(fb.instanceFieldOf(fb.referenceHandle(lambdaForm), reflection.lambdaFormMemberNameField));
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.referenceHandle(memberName), reflection.memberNameExactDispatcherField));
            // pass the method handle to the lambda form
            ArrayList<Value> newArgs = new ArrayList<>(arguments.size() + 1);
            newArgs.add(instance);
            newArgs.addAll(arguments);
            // todo: assume a static method or static dispatch helper
            InstanceMethodType methodType = (InstanceMethodType) target.getExecutable().getType();
            List<ValueType> parameterTypes = methodType.getParameterTypes();
            StaticMethodType dispatcherType = ts.getStaticMethodType(methodType.getReturnType(), parameterTypes).withFirstParameterType(methodType.getReceiverType());
            // cast to the matching static method pointer type for this particular helper shape
            Value castMethodPtr = fb.bitCast(methodPtr, dispatcherType.getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), newArgs));
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "invokeBasic", objArrayToObj, new InstanceIntrinsicMethodBodyFactory(invokeBasicMethodBody), 0);

        // MH.linkToStatic(..., memberName) → (static *memberName.vmentry)(...)

        StaticIntrinsic linkToStaticIntrinsic = (builder, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            int lastArg = arguments.size() - 1;
            Value memberNameValue = arguments.get(lastArg);
            if (memberNameValue instanceof ObjectLiteral memberNameLit) {
                Reflection reflection = Reflection.get(ctxt);
                BasicBlockBuilder fb = builder.getFirstBuilder();
                try {
                    VmObject memberName = memberNameLit.getValue();
                    Pointer methodPtr = memberName.getMemory().loadPointer(memberName.indexOf(reflection.memberNameExactDispatcherField), SinglePlain);
                    if (methodPtr instanceof StaticMethodPointer smp) {
                        // call the static method directly
                        return fb.call(fb.staticMethod(smp.getStaticMethod()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToStatic intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToStatic intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "linkToStatic", linkToStaticIntrinsic);

        StaticIntrinsic linkToStaticMethodBody = (builder, target, arguments) -> {
            Reflection reflection = Reflection.get(ctxt);
            BasicBlockBuilder fb = builder.getFirstBuilder();
            int lastArg = arguments.size() - 1;
            Value memberName = arguments.get(lastArg);
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.referenceHandle(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "linkToStatic", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToStaticMethodBody), 0);

        // MH.linkToInterface(..., memberName) → (static *memberName.vmentry)(...)

        StaticIntrinsic linkToInterfaceIntrinsic = (builder, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            int lastArg = arguments.size() - 1;
            Value memberNameValue = arguments.get(lastArg);
            if (memberNameValue instanceof ObjectLiteral memberNameLit) {
                Reflection reflection = Reflection.get(ctxt);
                BasicBlockBuilder fb = builder.getFirstBuilder();
                try {
                    VmObject memberName = memberNameLit.getValue();
                    Pointer methodPtr = memberName.getMemory().loadPointer(memberName.indexOf(reflection.memberNameExactDispatcherField), SinglePlain);
                    if (methodPtr instanceof StaticMethodPointer smp) {
                        // call the static helper method directly
                        return fb.call(fb.staticMethod(smp.getStaticMethod()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToInterface intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToInterface intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "linkToInterface", linkToInterfaceIntrinsic);

        StaticIntrinsic linkToInterfaceMethodBody = (builder, target, arguments) -> {
            Reflection reflection = Reflection.get(ctxt);
            BasicBlockBuilder fb = builder.getFirstBuilder();
            int lastArg = arguments.size() - 1;
            Value memberName = arguments.get(lastArg);
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.referenceHandle(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "linkToInterface", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToInterfaceMethodBody), 0);

        StaticIntrinsic linkToSpecialIntrinsic = (builder, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            int lastArg = arguments.size() - 1;
            Value memberNameValue = arguments.get(lastArg);
            if (memberNameValue instanceof ObjectLiteral memberNameLit) {
                Reflection reflection = Reflection.get(ctxt);
                BasicBlockBuilder fb = builder.getFirstBuilder();
                try {
                    VmObject memberName = memberNameLit.getValue();
                    Pointer methodPtr = memberName.getMemory().loadPointer(memberName.indexOf(reflection.memberNameExactDispatcherField), SinglePlain);
                    if (methodPtr instanceof StaticMethodPointer smp) {
                        // call the static helper method directly
                        return fb.call(fb.staticMethod(smp.getStaticMethod()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToSpecial intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToSpecial intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "linkToSpecial", linkToSpecialIntrinsic);

        StaticIntrinsic linkToSpecialMethodBody = (builder, target, arguments) -> {
            Reflection reflection = Reflection.get(ctxt);
            BasicBlockBuilder fb = builder.getFirstBuilder();
            int lastArg = arguments.size() - 1;
            Value memberName = arguments.get(lastArg);
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.referenceHandle(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "linkToSpecial", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToSpecialMethodBody), 0);

        StaticIntrinsic linkToVirtualIntrinsic = (builder, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            int lastArg = arguments.size() - 1;
            Value memberNameValue = arguments.get(lastArg);
            if (memberNameValue instanceof ObjectLiteral memberNameLit) {
                Reflection reflection = Reflection.get(ctxt);
                BasicBlockBuilder fb = builder.getFirstBuilder();
                try {
                    VmObject memberName = memberNameLit.getValue();
                    Pointer methodPtr = memberName.getMemory().loadPointer(memberName.indexOf(reflection.memberNameExactDispatcherField), SinglePlain);
                    if (methodPtr instanceof StaticMethodPointer smp) {
                        // call the static helper method directly
                        return fb.call(fb.staticMethod(smp.getStaticMethod()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToVirtual intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToVirtual intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.constructorOf(ie, internalErrorDesc, throwableToVoid), List.of(lf.literalOf(t.getThrowable())));
                    throw new BlockEarlyTermination(fb.throw_(ie));
                }
            } else {
                // do not expand intrinsic; let the method be called
                return null;
            }
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "linkToVirtual", linkToVirtualIntrinsic);

        StaticIntrinsic linkToVirtualMethodBody = (builder, target, arguments) -> {
            Reflection reflection = Reflection.get(ctxt);
            BasicBlockBuilder fb = builder.getFirstBuilder();
            int lastArg = arguments.size() - 1;
            Value memberName = arguments.get(lastArg);
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.referenceHandle(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(classContext, methodHandleInt, "linkToVirtual", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToVirtualMethodBody), 0);

        // VarHandle

        LoadedTypeDefinition vhDef = classContext.findDefinedType("java/lang/invoke/VarHandle").load();
        MethodElement getMethodHandle = vhDef.resolveMethodElementExact(
            "getMethodHandle",
            MethodDescriptor.synthesize(classContext, methodHandleDesc, List.of(BaseTypeDescriptor.I))
        );
        ClassTypeDescriptor varHandleDesc = vhDef.getDescriptor();

        final class VarHandleIntrinsic implements InstanceIntrinsic {
            private final int index;

            VarHandleIntrinsic(int index) {
                this.index = index;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, Value instance, InstanceMethodElementHandle target, List<Value> arguments) {
                // extract the method handle during build if possible
                LiteralFactory lf = ctxt.getLiteralFactory();
                Value methodHandle;
                if (instance instanceof ObjectLiteral objLit) {
                    Vm vm = Vm.requireCurrent();
                    VmObject mh = (VmObject) vm.invokeExact(getMethodHandle, objLit.getValue(), List.of(Integer.valueOf(index)));
                    methodHandle = lf.literalOf(mh);
                } else {
                    // todo: we may have to do something to pre-cache these...
                    methodHandle = builder.call(builder.exactMethodOf(instance, getMethodHandle), List.of(lf.literalOf(index)));
                }
                // now translate into an exact call on the method handle
                MethodDescriptor callSiteDesc = target.getCallSiteDescriptor();
                List<TypeDescriptor> parameterTypes = callSiteDesc.getParameterTypes();
                int paramCnt = parameterTypes.size();
                ArrayList<TypeDescriptor> modifiedParamTypes = new ArrayList<>(paramCnt + 1);
                modifiedParamTypes.add(varHandleDesc);
                modifiedParamTypes.addAll(parameterTypes);
                MethodDescriptor modifiedDesc = MethodDescriptor.synthesize(builder.getCurrentElement().getEnclosingType().getContext(),
                    callSiteDesc.getReturnType(), modifiedParamTypes);
                MethodElement invokeExact = ctxt.getBootstrapClassContext().findDefinedType("java/lang/invoke/MethodHandle").load().resolveMethodElementExact(
                    "invokeExact",
                    modifiedDesc
                );
                ArrayList<Value> modifiedArgs = new ArrayList<>(paramCnt + 1);
                modifiedArgs.add(instance);
                modifiedArgs.addAll(arguments);
                return builder.call(builder.exactMethodOf(methodHandle, invokeExact), modifiedArgs);
            }
        }

        // Keep this exact order, matching the order of the `java.lang.invoke.VarHandle.AccessMode` enumeration

        intrinsics.registerIntrinsic(varHandleDesc, "get", new VarHandleIntrinsic(0));
        intrinsics.registerIntrinsic(varHandleDesc, "set", new VarHandleIntrinsic(1));
        intrinsics.registerIntrinsic(varHandleDesc, "getVolatile", new VarHandleIntrinsic(2));
        intrinsics.registerIntrinsic(varHandleDesc, "setVolatile", new VarHandleIntrinsic(3));
        intrinsics.registerIntrinsic(varHandleDesc, "getAcquire", new VarHandleIntrinsic(4));
        intrinsics.registerIntrinsic(varHandleDesc, "setRelease", new VarHandleIntrinsic(5));
        intrinsics.registerIntrinsic(varHandleDesc, "getOpaque", new VarHandleIntrinsic(6));
        intrinsics.registerIntrinsic(varHandleDesc, "setOpaque", new VarHandleIntrinsic(7));
        intrinsics.registerIntrinsic(varHandleDesc, "compareAndSet", new VarHandleIntrinsic(8));
        intrinsics.registerIntrinsic(varHandleDesc, "compareAndExchange", new VarHandleIntrinsic(9));
        intrinsics.registerIntrinsic(varHandleDesc, "compareAndExchangeAcquire", new VarHandleIntrinsic(10));
        intrinsics.registerIntrinsic(varHandleDesc, "compareAndExchangeRelease", new VarHandleIntrinsic(11));
        intrinsics.registerIntrinsic(varHandleDesc, "weakCompareAndSetPlain", new VarHandleIntrinsic(12));
        intrinsics.registerIntrinsic(varHandleDesc, "weakCompareAndSet", new VarHandleIntrinsic(13));
        intrinsics.registerIntrinsic(varHandleDesc, "weakCompareAndSetAcquire", new VarHandleIntrinsic(14));
        intrinsics.registerIntrinsic(varHandleDesc, "weakCompareAndSetRelease", new VarHandleIntrinsic(15));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndSet", new VarHandleIntrinsic(16));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndSetAcquire", new VarHandleIntrinsic(17));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndSetRelease", new VarHandleIntrinsic(18));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndAdd", new VarHandleIntrinsic(19));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndAddAcquire", new VarHandleIntrinsic(20));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndAddRelease", new VarHandleIntrinsic(21));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseOr", new VarHandleIntrinsic(22));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseOrRelease", new VarHandleIntrinsic(23));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseOrAcquire", new VarHandleIntrinsic(24));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseAnd", new VarHandleIntrinsic(25));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseAndRelease", new VarHandleIntrinsic(26));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseAndAcquire", new VarHandleIntrinsic(27));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseXor", new VarHandleIntrinsic(28));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseXorRelease", new VarHandleIntrinsic(29));
        intrinsics.registerIntrinsic(varHandleDesc, "getAndBitwiseXorAcquire", new VarHandleIntrinsic(30));
    }

    /**
     * A convenience class that allows a method body to be expressed using the intrinsic API.
     */
    static final class StaticIntrinsicMethodBodyFactory implements MethodBodyFactory {
        private final StaticIntrinsic methodBodyIntrinsic;

        StaticIntrinsicMethodBodyFactory(StaticIntrinsic methodBodyIntrinsic) {
            this.methodBodyIntrinsic = methodBodyIntrinsic;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public MethodBody createMethodBody(int index, ExecutableElement element) {
            // hack the method to be hidden
            ((BasicElement)element).setModifierFlags(ClassFile.I_ACC_HIDDEN);
            // assume that the descriptor has *changed*
            InvokableType type = element.getType();
            DefinedTypeDefinition enclosingType = element.getEnclosingType();
            ClassContext classContext = enclosingType.getContext();
            BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(element);
            List<ParameterValue> paramValues = new ArrayList<>(type.getParameterCount());
            int cnt = type.getParameterCount();
            for (int i = 0; i < cnt; i ++) {
                paramValues.add(bbb.parameter(type.getParameterType(i), "p", i));
            }
            bbb.startMethod(paramValues);
            // build the entry block
            BlockLabel entryLabel = new BlockLabel();
            bbb.begin(entryLabel);
            // expand the "intrinsic"
            try {
                Value retVal = methodBodyIntrinsic.emitIntrinsic(bbb, (StaticMethodElementHandle) bbb.staticMethod((MethodElement) element), (List<Value>) (List) paramValues);
                if (retVal == null) {
                    throw new IllegalStateException("Failed to emit method body");
                }
                if (type.getReturnType() instanceof VoidType) {
                    bbb.return_();
                } else {
                    bbb.return_(retVal);
                }
            } catch (BlockEarlyTermination ignored) {}
            bbb.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            Schedule schedule = Schedule.forMethod(entryBlock);
            return MethodBody.of(entryBlock, schedule, null, paramValues);
        }
    }

    /**
     * A convenience class that allows a method body to be expressed using the intrinsic API.
     */
    static final class InstanceIntrinsicMethodBodyFactory implements MethodBodyFactory {
        private final InstanceIntrinsic methodBodyIntrinsic;

        InstanceIntrinsicMethodBodyFactory(InstanceIntrinsic methodBodyIntrinsic) {
            this.methodBodyIntrinsic = methodBodyIntrinsic;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public MethodBody createMethodBody(int index, ExecutableElement element) {
            // hack the method to be hidden
            ((BasicElement)element).setModifierFlags(ClassFile.I_ACC_HIDDEN);
            // assume that the descriptor has *changed*
            InvokableType type = element.getType();
            DefinedTypeDefinition enclosingType = element.getEnclosingType();
            ClassContext classContext = enclosingType.getContext();
            BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(element);
            ParameterValue this_ = bbb.parameter(element.getEnclosingType().load().getType().getReference(), "this", 0);
            List<ParameterValue> paramValues = new ArrayList<>(type.getParameterCount());
            int cnt = type.getParameterCount();
            for (int i = 0; i < cnt; i ++) {
                paramValues.add(bbb.parameter(type.getParameterType(i), "p", i));
            }
            bbb.startMethod(paramValues);
            // build the entry block
            BlockLabel entryLabel = new BlockLabel();
            bbb.begin(entryLabel);
            // expand the "intrinsic"
            try {
                Value retVal = methodBodyIntrinsic.emitIntrinsic(bbb, this_, (InstanceMethodElementHandle) bbb.exactMethodOf(this_, (MethodElement) element), (List<Value>) (List) paramValues);
                if (retVal == null) {
                    throw new IllegalStateException("Failed to emit method body");
                }
                if (type.getReturnType() instanceof VoidType) {
                    bbb.return_();
                } else {
                    bbb.return_(retVal);
                }
            } catch (BlockEarlyTermination ignored) {}
            bbb.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            Schedule schedule = Schedule.forMethod(entryBlock);
            return MethodBody.of(entryBlock, schedule, this_, paramValues);
        }
    }
}
