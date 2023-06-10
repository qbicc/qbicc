package org.qbicc.plugin.reflection;


import static org.qbicc.graph.atomic.AccessModes.*;

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
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.InstanceMethodLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
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
import org.qbicc.type.ObjectType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.VerifyFailedException;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticMethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

public final class ReflectionIntrinsics {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.reflection");

    private ReflectionIntrinsics() {}

    public static void register(CompilationContext ctxt) {
        registerReflectionAnalysisIntrinsics(ctxt);

        Intrinsics intrinsics = Intrinsics.get(ctxt);
        Patcher patcher = Patcher.get(ctxt);
        ClassContext bootstrapClassContext = ctxt.getBootstrapClassContext();

        String methodHandleInt = "java/lang/invoke/MethodHandle";
        String varHandleInt = "java/lang/invoke/VarHandle";
        ClassTypeDescriptor methodHandleDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, methodHandleInt);
        ClassTypeDescriptor varHandleDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, varHandleInt);
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "java/lang/Object");
        ClassTypeDescriptor internalErrorDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "java/lang/InternalError");
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "java/lang/Throwable");

        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(bootstrapClassContext, objDesc);

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.V, List.of());
        MethodDescriptor objArrayToObj = MethodDescriptor.synthesize(bootstrapClassContext, objDesc, List.of(objArrayDesc));
        MethodDescriptor objArrayToVoid = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.V, List.of(objArrayDesc));
        MethodDescriptor objArrayToBool = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.Z, List.of(objArrayDesc));
        MethodDescriptor throwableToVoid = MethodDescriptor.synthesize(bootstrapClassContext, BaseTypeDescriptor.V, List.of(throwableDesc));


        // mh.invoke(...) → mh.asType(actualType).invokeExact(...)

        InstanceIntrinsic invokeIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            // perform a build-time check
            InstanceMethodElement element = target.getExecutable();
            MethodDescriptor callSiteDescriptor = element.getDescriptor();
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Vm vm = Vm.requireCurrent();
            ClassContext classContext = builder.getCurrentClassContext();
            // the method type is constant, based on the call site
            VmObject realType;
            try {
                realType = vm.createMethodType(classContext, callSiteDescriptor);
            } catch (Thrown t) {
                ctxt.warning(fb.getLocation(), "Failed to create method type: %s", t);
                log.warnf(t, "Failed to create method type");
                throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
            }
            Value realHandle;
            LoadedTypeDefinition mhDef = element.getEnclosingType().load();
            int asTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("asType"));
            InstanceMethodElement asType = (InstanceMethodElement) mhDef.getMethod(asTypeIdx);
            if (instance instanceof ObjectLiteral instanceLit) {
                // the handle itself is a literal, so we can change its type immediately
                try {
                    realHandle = lf.literalOf((VmObject) vm.invokeVirtual(asType, instanceLit.getValue(), List.of(realType)));
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invoke intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.invoke intrinsic");
                    throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
                }
            } else {
                // the handle is not a literal, so change its type at run time
                realHandle = fb.call(fb.lookupVirtualMethod(instance, asType), instance, List.of(lf.literalOf(realType)));
            }
            // and transform to `invokeExact`
            Value invokeExactHandle = fb.resolveInstanceMethod(methodHandleDesc, "invokeExact", callSiteDescriptor);
            return fb.call(invokeExactHandle, realHandle, arguments);
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "invoke", invokeIntrinsic);

        // mh.invokeExact(...) → mh.checkType(type); mh.invokeBasic(...)

        InstanceIntrinsic invokeExactIntrinsic = (builder, instance, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            InstanceMethodElement element = target.getExecutable();
            MethodDescriptor callSiteDescriptor = element.getDescriptor();
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Vm vm = Vm.requireCurrent();
            ClassContext classContext = builder.getCurrentClassContext();
            // the method type is constant, based on the call site
            VmObject realType;
            try {
                realType = vm.createMethodType(classContext, callSiteDescriptor);
            } catch (Thrown t) {
                ctxt.warning(fb.getLocation(), "Failed to create method type: %s", t);
                log.warnf(t, "Failed to create method type");
                throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
            }
            LoadedTypeDefinition mhDef = element.getEnclosingType().load();
            int checkTypeIdx = mhDef.findMethodIndex(me -> me.nameEquals("checkType"));
            InstanceMethodElement checkType = (InstanceMethodElement) mhDef.getMethod(checkTypeIdx);
            if (instance instanceof ObjectLiteral instanceLit) {
                // the handle itself is a literal, so we can check its type immediately
                try {
                    vm.invokeExact(checkType, instanceLit.getValue(), List.of(realType));
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invokeExact intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.invokeExact intrinsic");
                    throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
                }
            } else {
                // the handle is not a literal, so check its type at run time
                fb.call(fb.lookupVirtualMethod(instance, checkType), instance, List.of(lf.literalOf(realType)));
            }
            // type is OK; now we can forward to invokeBasic(...) on this same instance
            Value invokeBasicHandle = fb.resolveInstanceMethod(methodHandleDesc, "invokeBasic", callSiteDescriptor);
            return fb.call(invokeBasicHandle, instance, arguments);
        };
        intrinsics.registerIntrinsic(methodHandleDesc, "invokeExact", invokeExactIntrinsic);

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
                        return fb.call(lf.literalOf(smp.getExecutableElement()), newArgs);
                    } else if (methodPtr instanceof InstanceMethodPointer imp) {
                        // todo: for now, we're using static helpers only
                        // call the exact method directly
                        return fb.call(lf.literalOf(imp.getExecutableElement()), arguments.get(0), newArgs.subList(1, newArgs.size()));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.invokeBasic intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.invokeBasic intrinsic");
                    Value ie = fb.new_(internalErrorDesc);
                    fb.call(fb.resolveConstructor(internalErrorDesc, throwableToVoid), ie, List.of(lf.literalOf(t.getThrowable())));
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
            InstanceMethodType methodType = target.getExecutable().getType();
            List<ValueType> parameterTypes = methodType.getParameterTypes();
            StaticMethodType dispatcherType = ts.getStaticMethodType(methodType.getReturnType(), parameterTypes).withFirstParameterType(methodType.getReceiverType());
            // cast to the matching static method pointer type for this particular helper shape
            Value castMethodPtr = fb.bitCast(methodPtr, dispatcherType.getPointer());
            throw new BlockEarlyTermination(fb.tailCall(castMethodPtr, newArgs));
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
                        return fb.call(lf.literalOf(smp.getExecutableElement()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToStatic intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToStatic intrinsic");
                    throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
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
            StaticMethodType methodType = target.getExecutable().getType();
            // cast to the matching static method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(castMethodPtr, arguments.subList(0, lastArg)));
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
                        return fb.call(lf.literalOf(smp.getExecutableElement()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToInterface intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToInterface intrinsic");
                    throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
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
            StaticMethodType methodType = target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(castMethodPtr, arguments.subList(0, lastArg)));
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
                        return fb.call(lf.literalOf(smp.getExecutableElement()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToSpecial intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToSpecial intrinsic");
                    throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
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
            StaticMethodType methodType = target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(castMethodPtr, arguments.subList(0, lastArg)));
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
                        return fb.call(lf.literalOf(smp.getExecutableElement()), arguments.subList(0, lastArg));
                    } else {
                        // ???
                        ctxt.warning(fb.getLocation(), "Unknown method handle pointer type: %s", methodPtr.getClass());
                        return null;
                    }
                } catch (Thrown t) {
                    ctxt.warning(fb.getLocation(), "Failed to expand MethodHandle.linkToVirtual intrinsic: %s", t);
                    log.warnf(t, "Failed to expand MethodHandle.linkToVirtual intrinsic");
                    throw new BlockEarlyTermination(fb.throw_(lf.literalOf(t.getThrowable())));
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
            StaticMethodType methodType = target.getExecutable().getType();
            // cast to the matching static helper method pointer type for this particular shape
            Value castMethodPtr = fb.bitCast(methodPtr, methodType.trimLastParameter().getPointer());
            throw new BlockEarlyTermination(fb.tailCall(castMethodPtr, arguments.subList(0, lastArg)));
        };
        patcher.replaceMethodBody(bootstrapClassContext, methodHandleInt, "linkToVirtual", objArrayToObj, new StaticIntrinsicMethodBodyFactory(linkToVirtualMethodBody), 0);

        // VarHandle

        intrinsics.registerIntrinsic(varHandleDesc, "fullFence", emptyToVoid, (builder, targetPtr, arguments) -> {
            builder.fence(GlobalSeqCst);
            return builder.emptyVoid();
        });

        intrinsics.registerIntrinsic(varHandleDesc, "acquireFence", emptyToVoid, (builder, targetPtr, arguments) -> {
            builder.fence(GlobalAcquire);
            return builder.emptyVoid();
        });

        intrinsics.registerIntrinsic(varHandleDesc, "releaseFence", emptyToVoid, (builder, targetPtr, arguments) -> {
            builder.fence(GlobalRelease);
            return builder.emptyVoid();
        });

        intrinsics.registerIntrinsic(varHandleDesc, "loadLoadFence", emptyToVoid, (builder, targetPtr, arguments) -> {
            builder.fence(GlobalLoadLoad);
            return builder.emptyVoid();
        });

        intrinsics.registerIntrinsic(varHandleDesc, "storeStoreFence", emptyToVoid, (builder, targetPtr, arguments) -> {
            builder.fence(GlobalStoreStore);
            return builder.emptyVoid();
        });

        LoadedTypeDefinition wmteDef = bootstrapClassContext.findDefinedType("java/lang/invoke/WrongMethodTypeException").load();
        ConstructorElement wmteCtor = wmteDef.requireSingleConstructor(ce -> ce.getParameters().isEmpty());

        final class VarHandleBodyIntrinsic implements InstanceIntrinsic {
            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, Value instance, InstanceMethodLiteral target, List<Value> arguments) {
                MethodElement methodElement = target.getExecutable();
                MethodDescriptor descriptor = methodElement.getDescriptor();
                DefinedTypeDefinition enclosingType = methodElement.getEnclosingType();
                if (Reflection.isErased(descriptor)) {
                    // base method implementation with erased type throws WrongMethodTypeException; overridden in subclasses
                    Value ex = builder.new_((ClassTypeDescriptor) wmteDef.getDescriptor());
                    builder.call(builder.getLiteralFactory().literalOf(wmteCtor), ex, List.of());
                    throw new BlockEarlyTermination(builder.throw_(ex));
                } else {
                    // not erased; delegate to erased version
                    ClassContext classContext = target.getExecutable().getEnclosingType().getContext();
                    MethodDescriptor erased = Reflection.erase(classContext, descriptor);
                    TypeDescriptor returnTypeDesc = descriptor.getReturnType();
                    if (Reflection.isErased(returnTypeDesc)) {
                        // no cast needed
                        throw new BlockEarlyTermination(builder.tailCall(builder.lookupVirtualMethod(instance, enclosingType.getDescriptor(), methodElement.getName(), erased), instance, arguments));
                    } else {
                        Value result = builder.call(builder.lookupVirtualMethod(instance, enclosingType.getDescriptor(), methodElement.getName(), erased), instance, arguments);
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
     * This collection of intrinsics implements automatic registration/folding of reflective operations
     * with constant arguments. Like GraalVM's "Automatic Detection"
     * of reflection (https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/),
     * the goal is to support simple patterns for reflection without explicit user annotation.
     */
    public static void registerReflectionAnalysisIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor jlsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor cnfeDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassNotFoundException");
        ClassTypeDescriptor jlrCtorDesc =  ClassTypeDescriptor.synthesize(classContext, "java/lang/reflect/Constructor");
        ClassTypeDescriptor jlrFieldDesc =  ClassTypeDescriptor.synthesize(classContext, "java/lang/reflect/Field");
        ClassTypeDescriptor jlrMethodDesc =  ClassTypeDescriptor.synthesize(classContext, "java/lang/reflect/Method");
        ArrayTypeDescriptor jlcADesc = ArrayTypeDescriptor.of(classContext, jlcDesc);

        MethodDescriptor stringToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of(jlsDesc));
        MethodDescriptor stringToField = MethodDescriptor.synthesize(classContext, jlrFieldDesc, List.of(jlsDesc));
        MethodDescriptor stringAndClassesToMethod = MethodDescriptor.synthesize(classContext, jlrMethodDesc, List.of(jlsDesc, jlcADesc));
        MethodDescriptor classesToCtor = MethodDescriptor.synthesize(classContext, jlrCtorDesc, List.of(jlcADesc));

        StaticIntrinsic forName = (builder, target, arguments) -> {
            if (arguments.get(0) instanceof StringLiteral sl) {
                String internalName = sl.getValue().replace('.', '/');
                DefinedTypeDefinition dtd = builder.getCurrentClassContext().findDefinedType(internalName);
                if (dtd != null) {
                    final LoadedTypeDefinition loaded;
                    final BasicBlockBuilder fb = builder.getFirstBuilder();
                    try {
                        loaded = dtd.load();
                    } catch (VerifyFailedException e) {
                        // it will always fail
                        final Value ex = fb.new_(cnfeDesc);
                        fb.call(fb.resolveConstructor(cnfeDesc, MethodDescriptor.VOID_METHOD_DESCRIPTOR), ex, List.of());
                        throw new BlockEarlyTermination(fb.throw_(ex));
                    }
                    Value cls = fb.classOf(loaded.getObjectType());
                    fb.initializeClass(cls); // Class.forName causes class initialization; preserve those semantics for interpreter
                    return cls;
                }
            }
            return null; // Allow call to proceed; can't optimize at compile time.
        };
        intrinsics.registerIntrinsic(jlcDesc, "forName", stringToClass, forName);

        InstanceIntrinsic findField = (builder, instance, target, arguments) -> {
            if (instance instanceof ClassOf co && co.getInput() instanceof TypeIdLiteral tl && tl.getValue() instanceof ObjectType ot) {
                LoadedTypeDefinition receivingClass = ot.getDefinition().load();
                if (arguments.get(0) instanceof StringLiteral sl) {
                    FieldElement fe = receivingClass.findField(sl.getValue());
                    if (fe != null) {
                        VmObject fObj = Reflection.get(builder.getContext()).getField(fe);
                        return builder.getContext().getLiteralFactory().literalOf(fObj);
                    }
                }
                // Not able to resolve; ensure receivingClass is ready for runtime reflection on its fields
                ReflectiveElementRegistry.get(builder.getContext()).bulkRegisterElementsForReflection(receivingClass, true, false, false);
            }
            return null;
        };
        intrinsics.registerIntrinsic(jlcDesc, "getField", stringToField, findField);
        intrinsics.registerIntrinsic(jlcDesc, "getDeclaredField", stringToField, findField);

        InstanceIntrinsic findMethod = (builder, instance, target, arguments) -> {
            if (instance instanceof ClassOf co && co.getInput() instanceof TypeIdLiteral tl && tl.getValue() instanceof ObjectType ot) {
                LoadedTypeDefinition receivingClass = ot.getDefinition().load();

                // TODO: To eliminate the reflection, we need arguments(0) to be a StringLiteral and
                //       arguments(1) to be an ArrayLiteral all of whose elements are ClassLiterals

                // Not able to resolve at compile time; conservatively register all methods of the class as potentially being invoked reflectively
                ReflectiveElementRegistry.get(ctxt).bulkRegisterElementsForReflection(receivingClass, false, false, true);
            }
            return null;
        };
        intrinsics.registerIntrinsic(jlcDesc, "getMethod", stringAndClassesToMethod, findMethod);
        intrinsics.registerIntrinsic(jlcDesc, "getDeclaredMethod", stringAndClassesToMethod, findMethod);


        InstanceIntrinsic findConstructor = (builder, instance, target, arguments) -> {
            if (instance instanceof ClassOf co && co.getInput() instanceof TypeIdLiteral tl && tl.getValue() instanceof ObjectType ot) {
                LoadedTypeDefinition receivingClass = ot.getDefinition().load();

                // TODO: To eliminate the reflection, we need arguments(0) to be an ArrayLiteral all of whose elements are ClassLiterals

                // Not able to resolve at compile time; conservatively register all constructors of the class as potentially being invoked reflectively
                ReflectiveElementRegistry.get(ctxt).bulkRegisterElementsForReflection(receivingClass, false, true, false);
            }
            return null;
        };
        intrinsics.registerIntrinsic(jlcDesc, "getConstructor", classesToCtor, findConstructor);
        intrinsics.registerIntrinsic(jlcDesc, "getDeclaredConstructor", classesToCtor, findConstructor);
    }

    /**
     * A convenience class that allows a method body to be expressed using the intrinsic API.
     */
    static final class StaticIntrinsicMethodBodyFactory implements MethodBodyFactory {
        private final StaticIntrinsic methodBodyIntrinsic;

        StaticIntrinsicMethodBodyFactory(StaticIntrinsic methodBodyIntrinsic) {
            this.methodBodyIntrinsic = methodBodyIntrinsic;
        }

        @Override
        public MethodBody createMethodBody(int index, ExecutableElement element) {
            if (! (element instanceof StaticMethodElement sme)) {
                throw new IllegalStateException();
            }
            // hack the method to be hidden
            sme.setModifierFlags(ClassFile.I_ACC_HIDDEN);
            // assume that the descriptor has *changed*
            InvokableType type = element.getType();
            DefinedTypeDefinition enclosingType = element.getEnclosingType();
            ClassContext classContext = enclosingType.getContext();
            LiteralFactory lf = classContext.getLiteralFactory();
            BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(element);
            // build the entry block
            BlockLabel entryLabel = new BlockLabel();
            bbb.begin(entryLabel);
            List<Value> paramValues = new ArrayList<>(type.getParameterCount());
            int cnt = type.getParameterCount();
            for (int i = 0; i < cnt; i ++) {
                paramValues.add(bbb.addParam(entryLabel, Slot.funcParam(i), type.getParameterType(i)));
            }
            // expand the "intrinsic"
            try {
                Value retVal = methodBodyIntrinsic.emitIntrinsic(bbb, lf.literalOf(sme), paramValues);
                if (retVal == null) {
                    throw new IllegalStateException("Failed to emit method body");
                }
                bbb.return_(retVal);
            } catch (BlockEarlyTermination ignored) {}
            bbb.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            return MethodBody.of(entryBlock, Slot.simpleArgList(cnt));
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

        @Override
        public MethodBody createMethodBody(int index, ExecutableElement element) {
            if (! (element instanceof InstanceMethodElement ime)) {
                throw new IllegalStateException();
            }
            // hack the method to be hidden
            ime.setModifierFlags(ClassFile.I_ACC_HIDDEN);
            // assume that the descriptor has *changed*
            InvokableType type = element.getType();
            DefinedTypeDefinition enclosingType = element.getEnclosingType();
            ClassContext classContext = enclosingType.getContext();
            LiteralFactory lf = classContext.getLiteralFactory();
            BasicBlockBuilder bbb = classContext.newBasicBlockBuilder(element);
            // build the entry block
            BlockLabel entryLabel = new BlockLabel();
            bbb.begin(entryLabel);
            BlockParameter this_ = bbb.addParam(entryLabel, Slot.this_(), element.getEnclosingType().load().getObjectType().getReference(), false);
            List<Value> paramValues = new ArrayList<>(type.getParameterCount());
            int cnt = type.getParameterCount();
            for (int i = 0; i < cnt; i ++) {
                paramValues.add(bbb.addParam(entryLabel, Slot.funcParam(i), type.getParameterType(i)));
            }
            // expand the "intrinsic"
            try {
                Value retVal = methodBodyIntrinsic.emitIntrinsic(bbb, this_, lf.literalOf(ime), paramValues);
                if (retVal == null) {
                    throw new IllegalStateException("Failed to emit method body");
                }
                bbb.return_(retVal);
            } catch (BlockEarlyTermination ignored) {}
            bbb.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            return MethodBody.of(entryBlock, Slot.simpleArgList(cnt));
        }
    }
}
