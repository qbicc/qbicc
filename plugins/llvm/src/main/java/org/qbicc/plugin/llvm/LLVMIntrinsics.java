package org.qbicc.plugin.llvm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.AsmLiteral;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Load;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StaticMethodLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.ArrayType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

public final class LLVMIntrinsics {
    public static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor buildTargetDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build$Target");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        StaticIntrinsic isLlvm = (builder, targetPtr, arguments) -> ctxt.getLiteralFactory().literalOf(true);
        intrinsics.registerIntrinsic(buildTargetDesc, "isLlvm", emptyToBool, isLlvm);

        // inline assembly
        ClassTypeDescriptor llvmRuntimeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/llvm/LLVM");
        //    public static native <T extends object> T asm(Class<T> returnType, String instruction, String operands, int flags, object... args);

        ClassTypeDescriptor thingDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$object");
        ClassTypeDescriptor vaListDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdarg$va_list");
        ClassTypeDescriptor vaListPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdarg$va_list_ptr");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ArrayTypeDescriptor arrayOfThingDesc = ArrayTypeDescriptor.of(classContext, thingDesc);

        MethodDescriptor asmDesc = MethodDescriptor.synthesize(classContext,
            thingDesc,
            List.of(
                classDesc,
                stringDesc,
                stringDesc,
                BaseTypeDescriptor.I,
                arrayOfThingDesc
            )
        );
        MethodDescriptor vaListClassToThing = MethodDescriptor.synthesize(classContext, thingDesc, List.of(vaListDesc, classDesc));
        MethodDescriptor vaListToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(vaListDesc));
        MethodDescriptor vaListVaListToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(vaListDesc, vaListDesc));
        MethodDescriptor vaListPtrToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(vaListPtrDesc));
        MethodDescriptor vaListPtrVaListPtrToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(vaListPtrDesc, vaListPtrDesc));

        intrinsics.registerIntrinsic(llvmRuntimeDesc, "asm", asmDesc, LLVMIntrinsics::asm);

        // replace Stdarg methods with intrinsics; like the corresponding C macros, they take a va_list rather than a pointer to it

        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        ClassTypeDescriptor stdArgDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdarg");

        StaticIntrinsic saVaStart = (builder, targetPtr, arguments) -> {
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Value vaListPtr;
            Value vaList = arguments.get(0);
            if (vaList instanceof Load load) {
                vaListPtr = load.getPointer();
            } else {
                ctxt.error(builder.getLocation(), "Invalid ap argument to va_start: must have an address");
                return voidLiteral;
            }
            return fb.call(fb.resolveStaticMethod(llvmRuntimeDesc, "va_start", vaListPtrToVoid), List.of(vaListPtr));
        };

        intrinsics.registerIntrinsic(stdArgDesc, "va_start", vaListToVoid, saVaStart);

        StaticIntrinsic saVaEnd = (builder, targetPtr, arguments) -> {
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Value vaListPtr;
            Value vaList = arguments.get(0);
            if (vaList instanceof Load load) {
                vaListPtr = load.getPointer();
            } else {
                ctxt.error(builder.getLocation(), "Invalid ap argument to va_end: must have an address");
                return voidLiteral;
            }
            return fb.call(fb.resolveStaticMethod(llvmRuntimeDesc, "va_end", vaListPtrToVoid), List.of(vaListPtr));
        };

        intrinsics.registerIntrinsic(stdArgDesc, "va_end", vaListToVoid, saVaEnd);

        StaticIntrinsic saVaCopy = (builder, targetPtr, arguments) -> {
            BasicBlockBuilder fb = builder.getFirstBuilder();
            Value destPtr;
            Value destList = arguments.get(0);
            if (destList instanceof Load load) {
                destPtr = load.getPointer();
            } else {
                ctxt.error(builder.getLocation(), "Invalid dest argument to va_copy: must have an address");
                return voidLiteral;
            }
            Value srcPtr;
            Value srcList = arguments.get(1);
            if (srcList instanceof Load load2) {
                srcPtr = load2.getPointer();
            } else {
                ctxt.error(builder.getLocation(), "Invalid src argument to va_copy: must have an address");
                return voidLiteral;
            }
            return fb.call(fb.resolveStaticMethod(llvmRuntimeDesc, "va_copy", vaListPtrVaListPtrToVoid), List.of(destPtr, srcPtr));
        };

        intrinsics.registerIntrinsic(stdArgDesc, "va_copy", vaListVaListToVoid, saVaCopy);

        // this one is technically implementation-neutral, but we can keep it here until we have another backend
        StaticIntrinsic saVaArg = (builder, targetPtr, arguments) -> {
            Value vaListPtr;
            Value vaList = arguments.get(0);
            if (vaList instanceof Load load) {
                vaListPtr = load.getPointer();
            } else {
                ctxt.error(builder.getLocation(), "Invalid ap argument to va_arg: must have an address");
                throw new BlockEarlyTermination(builder.unreachable());
            }
            Value outputType = arguments.get(1);
            if (outputType instanceof ClassOf co && co.getInput() instanceof TypeIdLiteral tl) {
                return builder.vaArg(vaListPtr, tl.getValue());
            } else {
                ctxt.error(builder.getLocation(), "Invalid type argument to va_arg (must be a class literal)");
                throw new BlockEarlyTermination(builder.unreachable());
            }
        };

        intrinsics.registerIntrinsic(stdArgDesc, "va_arg", vaListClassToThing, saVaArg);

        // Floating-point conversion intrinsics implemented by LLVM

        ClassTypeDescriptor cNativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");

        MethodDescriptor floatToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.F));
        MethodDescriptor floatToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.F));
        MethodDescriptor doubleToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.D));
        MethodDescriptor doubleToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.D));

        StaticIntrinsic floatToInt1 = (builder, targetPtr, arguments) -> {
            TypeSystem ts = ctxt.getTypeSystem();
            LiteralFactory lf = ctxt.getLiteralFactory();
            FunctionType fnType = ts.getFunctionType(ts.getSignedInteger32Type(), List.of(ts.getFloat32Type()));
            ExecutableElement executable = ((ExecutableLiteral) targetPtr).getExecutable();
            FunctionDeclaration decl = ctxt.getOrAddProgramModule(builder.getRootElement()).declareFunction(executable, "llvm.fptosi.sat.i32.f32", fnType, Function.FN_NO_SIDE_EFFECTS, SafePointBehavior.ALLOWED);
            return builder.getFirstBuilder().callNoSideEffects(lf.literalOf(decl), arguments);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "floatToInt1", floatToInt, floatToInt1);

        StaticIntrinsic floatToLong1 = (builder, targetPtr, arguments) -> {
            TypeSystem ts = ctxt.getTypeSystem();
            LiteralFactory lf = ctxt.getLiteralFactory();
            FunctionType fnType = ts.getFunctionType(ts.getSignedInteger64Type(), List.of(ts.getFloat32Type()));
            ExecutableElement executable = ((ExecutableLiteral) targetPtr).getExecutable();
            FunctionDeclaration decl = ctxt.getOrAddProgramModule(builder.getRootElement()).declareFunction(executable, "llvm.fptosi.sat.i64.f32", fnType, Function.FN_NO_SIDE_EFFECTS, SafePointBehavior.ALLOWED);
            return builder.getFirstBuilder().callNoSideEffects(lf.literalOf(decl), arguments);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "floatToLong1", floatToLong, floatToLong1);

        StaticIntrinsic doubleToInt1 = (builder, targetPtr, arguments) -> {
            TypeSystem ts = ctxt.getTypeSystem();
            LiteralFactory lf = ctxt.getLiteralFactory();
            FunctionType fnType = ts.getFunctionType(ts.getSignedInteger32Type(), List.of(ts.getFloat64Type()));
            ExecutableElement executable = ((ExecutableLiteral) targetPtr).getExecutable();
            FunctionDeclaration decl = ctxt.getOrAddProgramModule(builder.getRootElement()).declareFunction(executable, "llvm.fptosi.sat.i32.f64", fnType, Function.FN_NO_SIDE_EFFECTS, SafePointBehavior.ALLOWED);
            return builder.getFirstBuilder().callNoSideEffects(lf.literalOf(decl), arguments);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "doubleToInt1", doubleToInt, doubleToInt1);

        StaticIntrinsic doubleToLong1 = (builder, targetPtr, arguments) -> {
            TypeSystem ts = ctxt.getTypeSystem();
            LiteralFactory lf = ctxt.getLiteralFactory();
            FunctionType fnType = ts.getFunctionType(ts.getSignedInteger64Type(), List.of(ts.getFloat64Type()));
            ExecutableElement executable = ((ExecutableLiteral) targetPtr).getExecutable();
            FunctionDeclaration decl = ctxt.getOrAddProgramModule(builder.getRootElement()).declareFunction(executable, "llvm.fptosi.sat.i64.f64", fnType, Function.FN_NO_SIDE_EFFECTS, SafePointBehavior.ALLOWED);
            return builder.getFirstBuilder().callNoSideEffects(lf.literalOf(decl), arguments);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "doubleToLong1", doubleToLong, doubleToLong1);
    }

    // flag values must match the LLVM runtime API class.

    static final int ASM_FLAG_SIDE_EFFECT = 1 << 0;
    static final int ASM_FLAG_ALIGN_STACK = 1 << 1;
    static final int ASM_FLAG_INTEL_DIALECT = 1 << 2;
    static final int ASM_FLAG_UNWIND = 1 << 3;
    static final int ASM_FLAG_IMPLICIT_SIDE_EFFECT = 1 << 4;
    static final int ASM_FLAG_NO_RETURN = 1 << 5;

    /**
     * Handle an inline assembly statement.
     *
     * @param bb the block builder (must not be {@code null})
     * @param handle the input handle (must not be {@code null})
     * @param parameters the parameter values (must not be {@code null})
     * @return the assembly result (if any)
     */
    private static Value asm(final BasicBlockBuilder bb, final StaticMethodLiteral handle, final List<Value> parameters) {
        ExecutableElement element = bb.element();
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();

        Value returnTypeClazzValue = parameters.get(0);
        Value instructionValue = parameters.get(1);
        Value operandsValue = parameters.get(2);
        Value flagsValue = parameters.get(3);
        Value argsValue = parameters.get(4);

        // Determine the actual return type.
        // todo: also support VmClass literals
        ValueType returnType;
        if (returnTypeClazzValue instanceof ClassOf co && co.getInput() instanceof TypeIdLiteral tl) {
            returnType = tl.getValue();
        } else {
            ctxt.error(bb.getLocation(), "Type argument to `asm` must be a class literal or constant value");
            return lf.zeroInitializerLiteralOfType(ts.getVoidType());
        }

        // Get the instruction string.
        String instruction;
        if (instructionValue instanceof StringLiteral sl) {
            instruction = sl.getValue();
        } else if (instructionValue instanceof ObjectLiteral ol && ol.getValue() instanceof VmString vs) {
            instruction = vs.getContent();
        } else {
            ctxt.error(bb.getLocation(), "Instruction argument to `asm` must be a string literal or constant value");
            return lf.zeroInitializerLiteralOfType(ts.getVoidType());
        }

        // Get the operands string.
        String operands;
        if (operandsValue instanceof StringLiteral sl) {
            operands = sl.getValue();
        } else if (operandsValue instanceof ObjectLiteral ol && ol.getValue() instanceof VmString vs) {
            operands = vs.getContent();
        } else {
            ctxt.error(bb.getLocation(), "Operands argument to `asm` must be a string literal or constant value");
            return lf.zeroInitializerLiteralOfType(ts.getVoidType());
        }

        // Get the flags value.
        int flags;
        if (flagsValue instanceof IntegerLiteral il) {
            flags = il.intValue();
        } else {
            ctxt.error(bb.getLocation(), "Flags argument to `asm` must be an integer literal or constant value");
            return lf.zeroInitializerLiteralOfType(ts.getVoidType());
        }

        // Arguments.
        List<Value> args;
        FunctionType type;
        if (argsValue.getType() instanceof ArrayType at) {
            long length = at.getElementCount();
            if (length > 255) {
                ctxt.error(bb.getLocation(), "Too many arguments to `asm`");
                return lf.zeroInitializerLiteralOfType(ts.getVoidType());
            }
            if (length == 0) {
                // no arguments
                args = List.of();
                type = ts.getFunctionType(returnType, List.of());
            } else if (length == 1) {
                // just one argument
                args = List.of(bb.extractElement(argsValue, lf.literalOf(0)));
                type = ts.getFunctionType(returnType, List.of(args.get(0).getType()));
            } else {
                int cnt = (int) length;
                args = new ArrayList<>(cnt);
                List<ValueType> argTypes = new ArrayList<>(cnt);
                for (int i = 0; i < cnt; i ++) {
                    Value itemValue = bb.extractElement(argsValue, lf.literalOf(i));
                    args.add(itemValue);
                    argTypes.add(itemValue.getType());
                }
                type = ts.getFunctionType(returnType, argTypes);
            }
        } else {
            ctxt.error(bb.getLocation(), "Arguments to `asm` must be an immediate new array creation");
            return lf.zeroInitializerLiteralOfType(ts.getVoidType());
        }

        EnumSet<AsmLiteral.Flag> flagSet = EnumSet.of(AsmLiteral.Flag.NO_THROW);
        boolean generalSideEffects = false;
        if ((flags & ASM_FLAG_SIDE_EFFECT) != 0) {
            flagSet.add(AsmLiteral.Flag.SIDE_EFFECT);
            generalSideEffects = true;
        }
        if ((flags & ASM_FLAG_IMPLICIT_SIDE_EFFECT) != 0) {
            flagSet.add(AsmLiteral.Flag.IMPLICIT_SIDE_EFFECT);
            generalSideEffects = true;
        }
        if ((flags & ASM_FLAG_ALIGN_STACK) != 0) {
            flagSet.add(AsmLiteral.Flag.ALIGN_STACK);
        }
        if ((flags & ASM_FLAG_INTEL_DIALECT) != 0) {
            flagSet.add(AsmLiteral.Flag.INTEL_DIALECT);
        }
        if ((flags & ASM_FLAG_UNWIND) != 0) {
            flagSet.remove(AsmLiteral.Flag.NO_THROW);
            generalSideEffects = true;
        }
        boolean noReturn = (flags & ASM_FLAG_NO_RETURN) != 0;

        Value asm = lf.literalOfAsm(instruction, operands, type, flagSet.toArray(AsmLiteral.Flag[]::new));

        if (noReturn) {
            throw new BlockEarlyTermination(bb.callNoReturn(asm, args));
        }

        if (generalSideEffects) {
            return bb.call(asm, args);
        } else {
            return bb.callNoSideEffects(asm, args);
        }
    }
}
