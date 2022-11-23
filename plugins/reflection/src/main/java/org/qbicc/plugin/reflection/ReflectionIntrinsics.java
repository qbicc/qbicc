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
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.InstanceMethodElementHandle;
import org.qbicc.graph.Slot;
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
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ConstructorElement;
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
        ClassContext bootstrapClassContext = ctxt.getBootstrapClassContext();

        String methodHandleInt = "java/lang/invoke/MethodHandle";
        String varHandleInt = "java/lang/invoke/VarHandle";
        ClassTypeDescriptor methodHandleDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, methodHandleInt);
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "java/lang/Object");
        ClassTypeDescriptor internalErrorDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "java/lang/InternalError");
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "java/lang/Throwable");

        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(bootstrapClassContext, objDesc);

        MethodDescriptor objArrayToObj = MethodDescriptor.synthesize(bootstrapClassContext, objDesc, List.of(objArrayDesc));
        MethodDescriptor objArrayToVoid = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.V, List.of(objArrayDesc));
        MethodDescriptor objArrayToBool = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.Z, List.of(objArrayDesc));
        MethodDescriptor throwableToVoid = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.V, List.of(throwableDesc));


        // mh.invoke(...) → mh.asType(actualType).invokeExact(...)

        InstanceIntrinsic invokeIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (instance instanceof ObjectLiteral instanceLit) {
                // perform a build-time check
                MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
                BasicBlockBuilder fb = builder.getFirstBuilder();
                Vm vm = Vm.requireCurrent();
                try {
                    ClassContext classContext = target.getElement().getEnclosingType().getContext();
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
                ClassContext classContext = target.getElement().getEnclosingType().getContext();
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
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "invoke", objArrayToObj, new InstanceIntrinsicMethodBodyFactory(invokeMethodBody), 0);

        // mh.invokeExact(...) → mh.checkType(type); mh.invokeBasic(...)

        InstanceIntrinsic invokeExactIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (instance instanceof ObjectLiteral instanceLit) {
                MethodDescriptor callSiteDescriptor = target.getCallSiteDescriptor();
                BasicBlockBuilder fb = builder.getFirstBuilder();
                Vm vm = Vm.requireCurrent();
                try {
                    ClassContext classContext = target.getElement().getEnclosingType().getContext();
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
                ClassContext classContext = target.getElement().getEnclosingType().getContext();
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
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "invokeExact", objArrayToObj, new InstanceIntrinsicMethodBodyFactory(invokeExactMethodBody), 0);

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
            Value lambdaForm = fb.load(fb.instanceFieldOf(fb.decodeReference(instance), reflection.methodHandleLambdaFormField));
            Value memberName = fb.load(fb.instanceFieldOf(fb.decodeReference(lambdaForm), reflection.lambdaFormMemberNameField));
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.decodeReference(memberName), reflection.memberNameExactDispatcherField));
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
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "invokeBasic", objArrayToObj, new InstanceIntrinsicMethodBodyFactory(invokeBasicMethodBody), 0);

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
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.decodeReference(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "linkToStatic", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToStaticMethodBody), 0);

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
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.decodeReference(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "linkToInterface", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToInterfaceMethodBody), 0);

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
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.decodeReference(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "linkToSpecial", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToSpecialMethodBody), 0);

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
            Value methodPtr = fb.load(fb.instanceFieldOf(fb.decodeReference(memberName), reflection.memberNameExactDispatcherField));
            StaticMethodType methodType = (StaticMethodType) target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(fb.pointerHandle(castMethodPtr), arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "linkToVirtual", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToVirtualMethodBody), 0);

        // VarHandle

        LoadedTypeDefinition wmteDef = bootstrapClassContext.findDefinedType("java/lang/invoke/WrongMethodTypeException").load();
        ConstructorElement wmteCtor = wmteDef.requireSingleConstructor(ce -> ce.getParameters().isEmpty());

        final class VarHandleBodyIntrinsic implements InstanceIntrinsic {
            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, Value instance, InstanceMethodElementHandle target, List<Value> arguments) {
                MethodElement methodElement = target.getExecutable();
                MethodDescriptor descriptor = methodElement.getDescriptor();
                DefinedTypeDefinition enclosingType = methodElement.getEnclosingType();
                if (Reflection.isErased(descriptor)) {
                    // base method implementation with erased type throws WrongMethodTypeException; overridden in subclasses
                    Value ex = builder.new_((ClassTypeDescriptor) wmteDef.getDescriptor());
                    builder.call(builder.constructorOf(ex, wmteCtor), List.of());
                    throw new BlockEarlyTermination(builder.throw_(ex));
                } else {
                    // not erased; delegate to erased version
                    ClassContext classContext = target.getExecutable().getEnclosingType().getContext();
                    MethodDescriptor erased = Reflection.erase(classContext, descriptor);
                    TypeDescriptor returnTypeDesc = descriptor.getReturnType();
                    if (Reflection.isErased(returnTypeDesc)) {
                        // no cast needed
                        throw new BlockEarlyTermination(builder.tailCall(builder.virtualMethodOf(instance, enclosingType.getDescriptor(), methodElement.getName(), erased), arguments));
                    } else {
                        Value result = builder.call(builder.virtualMethodOf(instance, enclosingType.getDescriptor(), methodElement.getName(), erased), arguments);
                        throw new BlockEarlyTermination(builder.return_(builder.checkcast(result, returnTypeDesc)));
                    }
                }
            }

        }

        InstanceIntrinsicMethodBodyFactory varHandleBodyFactory = new InstanceIntrinsicMethodBodyFactory(new VarHandleBodyIntrinsic());

        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "get", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "set", objArrayToVoid, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getVolatile", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "setVolatile", objArrayToVoid, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAcquire", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "setRelease", objArrayToVoid, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getOpaque", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "setOpaque", objArrayToVoid, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "compareAndSet", objArrayToBool, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "compareAndExchange", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "compareAndExchangeAcquire", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "compareAndExchangeRelease", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "weakCompareAndSetPlain", objArrayToBool, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "weakCompareAndSet", objArrayToBool, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "weakCompareAndSetAcquire", objArrayToBool, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "weakCompareAndSetRelease", objArrayToBool, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndSet", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndSetAcquire", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndSetRelease", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndAdd", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndAddAcquire", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndAddRelease", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseOr", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseOrRelease", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseOrAcquire", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseAnd", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseAndRelease", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseAndAcquire", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseXor", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseXorRelease", objArrayToObj, varHandleBodyFactory, 0);
        patcher.replaceMethodBody(bootstrapClassContext, varHandleInt, "getAndBitwiseXorAcquire", objArrayToObj, varHandleBodyFactory, 0);
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
            // build the entry block
            BlockLabel entryLabel = new BlockLabel();
            bbb.begin(entryLabel);
            List<BlockParameter> paramValues = new ArrayList<>(type.getParameterCount());
            int cnt = type.getParameterCount();
            for (int i = 0; i < cnt; i ++) {
                paramValues.add(bbb.addParam(entryLabel, Slot.funcParam(i), type.getParameterType(i)));
            }
            // expand the "intrinsic"
            try {
                Value retVal = methodBodyIntrinsic.emitIntrinsic(bbb, (StaticMethodElementHandle) bbb.staticMethod((MethodElement) element), (List<Value>) (List) paramValues);
                if (retVal == null) {
                    throw new IllegalStateException("Failed to emit method body");
                }
                bbb.return_(retVal);
            } catch (BlockEarlyTermination ignored) {}
            bbb.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            Schedule schedule = Schedule.forMethod(entryBlock);
            return MethodBody.of(entryBlock, schedule, Slot.simpleArgList(cnt));
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
            // build the entry block
            BlockLabel entryLabel = new BlockLabel();
            bbb.begin(entryLabel);
            BlockParameter this_ = bbb.addParam(entryLabel, Slot.this_(), element.getEnclosingType().load().getObjectType().getReference(), false);
            List<BlockParameter> paramValues = new ArrayList<>(type.getParameterCount());
            int cnt = type.getParameterCount();
            for (int i = 0; i < cnt; i ++) {
                paramValues.add(bbb.addParam(entryLabel, Slot.funcParam(i), type.getParameterType(i)));
            }
            // expand the "intrinsic"
            try {
                Value retVal = methodBodyIntrinsic.emitIntrinsic(bbb, this_, (InstanceMethodElementHandle) bbb.exactMethodOf(this_, (MethodElement) element), (List<Value>) (List) paramValues);
                if (retVal == null) {
                    throw new IllegalStateException("Failed to emit method body");
                }
                bbb.return_(retVal);
            } catch (BlockEarlyTermination ignored) {}
            bbb.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            Schedule schedule = Schedule.forMethod(entryBlock);
            return MethodBody.of(entryBlock, schedule, Slot.simpleArgList(cnt));
        }
    }
}
