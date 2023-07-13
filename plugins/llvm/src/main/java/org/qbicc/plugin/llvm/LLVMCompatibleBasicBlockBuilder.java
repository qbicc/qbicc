package org.qbicc.plugin.llvm;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.unwind.UnwindExceptionStrategy;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NumericType;
import org.qbicc.type.PointerType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

public class LLVMCompatibleBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final LLVMConfiguration config;

    public LLVMCompatibleBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate, LLVMConfiguration config) {
        super(delegate);
        this.config = config;
        this.ctxt = getContext();
    }

    @Override
    public Value decodeReference(Value refVal, PointerType pointerType) {
        // this won't be scheduled by the LLVM backend when pointer strategy is in use
        return super.decodeReference(refVal, pointerType);
    }

    @Override
    public Value encodeReference(Value pointer, ReferenceType referenceType) {
        // this won't be scheduled by the LLVM backend when pointer strategy is in use
        return super.encodeReference(pointer, referenceType);
    }

    @Override
    public Value min(Value v1, Value v2) {
        return minMax(false, v1, v2);
    }

    @Override
    public Value max(Value v1, Value v2) {
        return minMax(true, v1, v2);
    }

    private Value minMax(boolean isMax, Value v1, Value v2) {
        TypeSystem tps = ctxt.getTypeSystem();
        BasicBlockBuilder fb = getFirstBuilder();
        NumericType numericType;
        String funcName = isMax ? "max" : "min";
        String fullFuncName;
        if (v1.getType() instanceof FloatType && v2.getType() instanceof FloatType) {
            FloatType t1 = (FloatType) v1.getType();
            FloatType t2 = (FloatType) v2.getType();
            // todo: CPU capability bits
            if (ctxt.getPlatform().getCpu() == Cpu.AARCH64) {
                numericType = (t1.getSize() == 4) ? tps.getFloat32Type() : tps.getFloat64Type();
                fullFuncName = "llvm." + funcName + "imum.f" + numericType.getMinBits();
                return minMaxIntrinsic(fullFuncName, numericType, v1, v2);
            } else {
                // we have to simulate it (poorly)
                Value lt1 = isLt(v1, v2);
                Value gt1 = isGt(v1, v2);
                Value notNan1 = isEq(v1, v1);
                Value notNan2 = isEq(v2, v2);
                Value bc1 = bitCast(v1, t1.getSameSizeSignedIntegerType());
                Value bc2 = bitCast(v2, t2.getSameSizeSignedIntegerType());
                Value last = bitCast(minMax(isMax, bc1, bc2), t1);
                return fb.select(isMax ? gt1 : lt1, v1, fb.select(isMax ? lt1 : gt1, v2, fb.select(notNan1, fb.select(notNan2, last, v2), v1)));
            }
        } else {
            if (v1.getType() instanceof SignedIntegerType && v2.getType() instanceof SignedIntegerType) {
                numericType = (v1.getType().getSize() == 4) ? tps.getSignedInteger32Type() : tps.getSignedInteger64Type();
                fullFuncName = "llvm.s" + funcName + ".i" + numericType.getMinBits();
                return minMaxIntrinsic(fullFuncName, numericType, v1, v2);
            } else if (v1.getType() instanceof UnsignedIntegerType && v2.getType() instanceof UnsignedIntegerType) {
                numericType = (v1.getType().getSize() == 4) ? tps.getUnsignedInteger32Type() : tps.getUnsignedInteger64Type();
                fullFuncName = "llvm.u" + funcName + ".i" + numericType.getMinBits();
                return minMaxIntrinsic(fullFuncName, numericType, v1, v2);
            }
            // Fallback for integer lengths other than 32 and 64 bits
            return fb.select(isMax ? fb.isGt(v1, v2) : fb.isLt(v1, v2), v1, v2);
        }
    }

    private Value minMaxIntrinsic(String funcName, NumericType numericType, Value v1, Value v2) {
        TypeSystem tps = ctxt.getTypeSystem();
        FunctionType functionType = tps.getFunctionType(numericType, numericType, numericType);
        FunctionDeclaration declaration = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, funcName, functionType, Function.FN_NO_SAFEPOINTS | Function.FN_NO_SIDE_EFFECTS);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return getFirstBuilder().callNoSideEffects(lf.literalOf(declaration), List.of(v1, v2));
    }

    @Override
    public Value byteSwap(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType, inputType);
        int minBits = inputType.getMinBits();
        if ((minBits & 0xF) != 0) {
            throw new IllegalArgumentException("Invalid integer type " + inputType + " for byte swap (must be a multiple of 16 bits)");
        }
        String functionName = "llvm.bswap.i" + minBits;
        FunctionDeclaration declaration = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, functionName, functionType, Function.FN_NO_SAFEPOINTS | Function.FN_NO_SIDE_EFFECTS);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return getFirstBuilder().callNoSideEffects(lf.literalOf(declaration), List.of(v));
    }

    @Override
    public Value bitReverse(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType, inputType);
        int minBits = inputType.getMinBits();
        String functionName = "llvm.bitreverse.i" + minBits;
        FunctionDeclaration declaration = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, functionName, functionType, Function.FN_NO_SAFEPOINTS | Function.FN_NO_SIDE_EFFECTS);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return getFirstBuilder().callNoSideEffects(lf.literalOf(declaration), List.of(v));
    }

    @Override
    public Value countLeadingZeros(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType.asUnsigned(), inputType, tps.getBooleanType());
        int minBits = inputType.getMinBits();
        String functionName = "llvm.ctlz.i" + minBits;
        FunctionDeclaration declaration = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, functionName, functionType, Function.FN_NO_SAFEPOINTS | Function.FN_NO_SIDE_EFFECTS);
        LiteralFactory lf = ctxt.getLiteralFactory();
        Value result = getFirstBuilder().callNoSideEffects(lf.literalOf(declaration), List.of(v, lf.literalOf(false)));
        // LLVM always returns the same type as the input
        if (minBits < 32) {
            result = getFirstBuilder().extend(result, tps.getUnsignedInteger32Type());
            result = getFirstBuilder().bitCast(result, tps.getSignedInteger32Type());
        } else if (minBits == 32) {
            result = getFirstBuilder().bitCast(result, tps.getSignedInteger32Type());
        } else {
            assert minBits > 32;
            result = getFirstBuilder().truncate(result, tps.getSignedInteger32Type());
        }
        return result;
    }

    @Override
    public Value countTrailingZeros(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType.asUnsigned(), inputType, tps.getBooleanType());
        int minBits = inputType.getMinBits();
        String functionName = "llvm.cttz.i" + minBits;
        FunctionDeclaration declaration = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, functionName, functionType, Function.FN_NO_SAFEPOINTS | Function.FN_NO_SIDE_EFFECTS);
        LiteralFactory lf = ctxt.getLiteralFactory();
        Value result = getFirstBuilder().callNoSideEffects(lf.literalOf(declaration), List.of(v, lf.literalOf(false)));
        // LLVM always returns the same type as the input
        if (minBits < 32) {
            result = getFirstBuilder().extend(result, tps.getUnsignedInteger32Type());
            result = getFirstBuilder().bitCast(result, tps.getSignedInteger32Type());
        } else if (minBits == 32) {
            result = getFirstBuilder().bitCast(result, tps.getSignedInteger32Type());
        } else {
            assert minBits > 32;
            result = getFirstBuilder().truncate(result, tps.getSignedInteger32Type());
        }
        return result;
    }

    @Override
    public Value populationCount(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType.asUnsigned(), inputType);
        int minBits = inputType.getMinBits();
        String functionName = "llvm.ctpop.i" + minBits;
        FunctionDeclaration declaration = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, functionName, functionType, Function.FN_NO_SAFEPOINTS | Function.FN_NO_SIDE_EFFECTS);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        Value result = getFirstBuilder().callNoSideEffects(lf.literalOf(declaration), List.of(v));
        // LLVM always returns the same type as the input
        if (minBits < 32) {
            result = getFirstBuilder().extend(result, tps.getUnsignedInteger32Type());
            result = getFirstBuilder().bitCast(result, tps.getSignedInteger32Type());
        } else if (minBits == 32) {
            result = getFirstBuilder().bitCast(result, tps.getSignedInteger32Type());
        } else {
            assert minBits > 32;
            result = getFirstBuilder().truncate(result, tps.getSignedInteger32Type());
        }
        return result;
    }

    @Override
    public Value negate(Value v) {
        if (v.getType() instanceof IntegerType) {
            final IntegerLiteral zero = ctxt.getLiteralFactory().literalOf((IntegerType) v.getType(), 0);
            return super.sub(zero, v);
        }
        
        return super.negate(v);
    }

    @Override
    public Value offsetOfField(FieldElement fieldElement) {
        if (fieldElement.isStatic()) {
            return ctxt.getLiteralFactory().literalOf(-1);
        } else {
            LayoutInfo layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(fieldElement.getEnclosingType());
            return ctxt.getLiteralFactory().literalOf(layoutInfo.getMember(fieldElement).getOffset());
        }
    }

    @Override
    public Value pointerDifference(Value leftPointer, Value rightPointer) {
        // this is similar to how clang translates pointer differences
        BasicBlockBuilder fb = getFirstBuilder();
        SignedIntegerType intType = leftPointer.getType(PointerType.class).getSameSizedSignedInteger();
        Value leftCast = fb.bitCast(leftPointer, intType);
        Value rightCast = fb.bitCast(rightPointer, intType);
        Value byteDiff = fb.sub(leftCast, rightCast);
        ValueType pointeeType = leftPointer.getPointeeType();
        long size = pointeeType.getSize();
        if (size <= 1) {
            // no scale needed (and, treat zero-sized pointers as byte pointers)
            return byteDiff;
        } else {
            return fb.divide(byteDiff, getLiteralFactory().literalOf(intType, size));
        }
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        Value loaded;
        if (accessMode.includes(GlobalAcquire)) {
            // we have to emit a global fence
            loaded = super.load(pointer, SingleUnshared);
            fence(accessMode.getGlobalAccess());
            return loaded;
        } else if (accessMode.includes(GlobalPlain)) {
            // emit equivalent single load
            return super.load(pointer, SinglePlain);
        } else if (accessMode.includes(GlobalUnshared)) {
            // emit equivalent single load
            return super.load(pointer, SingleUnshared);
        }
        // Break apart atomic structure and array loads
        if (pointer.getPointeeType() instanceof StructType st) {
            LiteralFactory lf = ctxt.getLiteralFactory();
            Value res = lf.zeroInitializerLiteralOfType(st);
            for (StructType.Member member : st.getPaddedMembers()) {
                res = insertMember(res, member, load(memberOf(pointer, member), accessMode));
            }
            return res;
        } else if (pointer.getPointeeType() instanceof ArrayType at) {
            long ec = at.getElementCount();
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (ec < 16) {
                // unrolled
                Value res = lf.zeroInitializerLiteralOfType(at);
                for (long i = 0; i < ec; i ++) {
                    IntegerLiteral idxLit = lf.literalOf(i);
                    res = insertElement(res, idxLit, load(elementOf(pointer, idxLit), accessMode));
                }
                return res;
            } else {
                // rolled loop
                BlockLabel top = new BlockLabel();
                BlockLabel exit = new BlockLabel();
                SignedIntegerType idxType = ctxt.getTypeSystem().getSignedInteger64Type();
                goto_(top, Map.of(Slot.temp(0), lf.literalOf(idxType, 0), Slot.temp(1), lf.zeroInitializerLiteralOfType(at)));
                begin(top);
                BlockParameter idx = addParam(top, Slot.temp(0), idxType);
                BlockParameter val = addParam(top, Slot.temp(1), at);
                Value modVal = insertElement(val, idx, load(elementOf(pointer, idx), accessMode));
                if_(isLt(idx, lf.literalOf(idxType, ec)), top, exit, Map.of(Slot.temp(0), add(idx, lf.literalOf(idxType, 1)), Slot.temp(1), modVal));
                begin(exit);
                // be sure to return the final modified value
                return addParam(exit, Slot.temp(1), at);
            }
        }
        return super.load(pointer, accessMode);
    }

    @Override
    public BasicBlock unreachable() {
        TypeSystem ts = ctxt.getTypeSystem();
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(getRootElement()).declareFunction(null, "llvm.trap", ts.getFunctionType(ts.getVoidType()));
        return callNoReturn(ctxt.getLiteralFactory().literalOf(decl), List.of());
    }

    @Override
    public Node store(Value pointer, Value value, WriteAccessMode accessMode) {
        if (pointer.getPointeeType() instanceof BooleanType) {
            ctxt.error("Invalid boolean-typed handle %s", pointer);
        }
        if (value.getType() instanceof BooleanType) {
            ctxt.error("Invalid boolean-typed value %s", value);
        }
        if (accessMode.includes(GlobalRelease)) {
            // we have to emit a global fence
            Node store = store(pointer, value, SingleUnshared);
            fence(accessMode.getGlobalAccess());
            return store;
        } else if (accessMode.includes(GlobalPlain)) {
            return store(pointer, value, SinglePlain);
        } else if (accessMode.includes(GlobalUnshared)) {
            return store(pointer, value, SingleUnshared);
        }
        // Break apart atomic structure and array stores
        if (pointer.getPointeeType() instanceof StructType st) {
            if (value instanceof Literal lit && lit.isZero()) {
                LiteralFactory lf = getLiteralFactory();
                for (StructType.Member member : st.getPaddedMembers()) {
                    store(memberOf(pointer, member), lf.zeroInitializerLiteralOfType(member.getType()), accessMode);
                }
                return nop();
            } else {
                for (StructType.Member member : st.getPaddedMembers()) {
                    store(memberOf(pointer, member), extractMember(value, member), accessMode);
                }
                return nop();
            }
        } else if (pointer.getPointeeType() instanceof ArrayType at) {
            long ec = at.getElementCount();
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (ec < 16) {
                // unrolled
                if (value instanceof Literal lit && lit.isZero()) {
                    for (long i = 0; i < ec; i ++) {
                        // temp: make sure that indexes compute equally; see https://github.com/qbicc/qbicc/issues/1103
                        IntegerLiteral idxLit = lf.literalOf((int) i);
                        store(elementOf(pointer, idxLit), lf.zeroInitializerLiteralOfType(at.getElementType()));
                    }
                } else {
                    for (long i = 0; i < ec; i ++) {
                        // temp: make sure that indexes compute equally; see https://github.com/qbicc/qbicc/issues/1103
                        IntegerLiteral idxLit = lf.literalOf((int) i);
                        store(elementOf(pointer, idxLit), extractElement(value, idxLit));
                    }
                }
                return nop();
            } else {
                // rolled loop
                BlockLabel top = new BlockLabel();
                BlockLabel exit = new BlockLabel();
                SignedIntegerType idxType = ctxt.getTypeSystem().getSignedInteger64Type();
                goto_(top, Slot.temp(0), lf.literalOf(idxType, 0));
                begin(top);
                BlockParameter idx = addParam(top, Slot.temp(0), idxType);
                if (value instanceof Literal lit && lit.isZero()) {
                    store(elementOf(pointer, idx), lf.zeroInitializerLiteralOfType(at.getElementType()));
                } else {
                    store(elementOf(pointer, idx), extractElement(value, idx));
                }
                final Value incIdx = add(idx, lf.literalOf(idxType, 1));
                if_(isLt(incIdx, lf.literalOf(idxType, ec)), top, exit, Map.of(Slot.temp(0), incIdx));
                begin(exit);
            }
            return nop();
        }
        return super.store(pointer, value, accessMode);
    }

    @Override
    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        BasicBlockBuilder fb = getFirstBuilder();
        StructType resultType = CmpAndSwap.getResultType(ctxt, pointer.getPointeeType());
        ReadAccessMode lowerReadMode = readMode;
        WriteAccessMode lowerWriteMode = writeMode;
        Value result;
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            // not actually atomic!
            // emit fences via load and store logic.
            Value compareVal = fb.load(pointer, readMode);
            BlockLabel success = new BlockLabel();
            BlockLabel resume = new BlockLabel();
            Value compareResult = fb.isEq(compareVal, expect);
            LiteralFactory lf = ctxt.getLiteralFactory();
            Literal zeroStruct = lf.zeroInitializerLiteralOfType(resultType);
            Value withCompareVal = fb.insertMember(zeroStruct, resultType.getMember(0), compareVal);
            result = fb.insertMember(withCompareVal, resultType.getMember(1), compareResult);
            fb.if_(compareResult, success, resume, Map.of());
            fb.begin(success);
            fb.store(pointer, update, writeMode);
            fb.goto_(resume, Map.of());
            fb.begin(resume);
        } else {
            boolean readRequiresFence = readMode.includes(GlobalAcquire);
            if (readMode instanceof GlobalAccessMode) {
                lowerReadMode = SingleOpaque;
            }
            boolean writeRequiresFence = writeMode.includes(GlobalRelease);
            if (writeMode instanceof GlobalAccessMode) {
                lowerWriteMode = SingleOpaque;
            }
            result = super.cmpAndSwap(pointer, expect, update, lowerReadMode, lowerWriteMode, strength);
            if (readRequiresFence) {
                fence(readMode.getGlobalAccess());
            }
            if (writeRequiresFence && ! (readRequiresFence && readMode.includes(writeMode))) {
                // the write side requires a global fence, but only in the success case, and only if the read fence is insufficient
                Value successFlag = fb.extractMember(result, resultType.getMember(1));
                BlockLabel success = new BlockLabel();
                BlockLabel resume = new BlockLabel();
                fb.if_(successFlag, success, resume, Map.of());
                fb.begin(success);
                fb.fence(writeMode.getGlobalAccess());
                fb.goto_(resume, Map.of());
                fb.begin(resume);
            }
        }

        return result;
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        BasicBlockBuilder fb = getFirstBuilder();
        ReadAccessMode lowerReadMode = readMode;
        WriteAccessMode lowerWriteMode = writeMode;
        Value result;
        if (GlobalPlain.includes(readMode) && GlobalPlain.includes(writeMode)) {
            // not actually atomic!
            // emit fences via load and store logic.
            result = fb.load(pointer, readMode);
            Value computed = switch (op) {
                case SET -> update;
                case ADD -> fb.add(result, update);
                case SUB -> fb.sub(result, update);
                case BITWISE_AND -> fb.and(result, update);
                case BITWISE_NAND -> fb.complement(fb.and(result, update));
                case BITWISE_OR -> fb.or(result, update);
                case BITWISE_XOR -> fb.xor(result, update);
                case MIN -> fb.min(result, update);
                case MAX -> fb.max(result, update);
            };
            fb.store(pointer, computed, writeMode);
        } else {
            boolean readRequiresFence = readMode.includes(GlobalAcquire);
            if (readMode instanceof GlobalAccessMode) {
                lowerReadMode = SingleOpaque;
            }
            boolean writeRequiresFence = writeMode.includes(GlobalRelease);
            if (writeMode instanceof GlobalAccessMode) {
                lowerWriteMode = SingleOpaque;
            }
            result = super.readModifyWrite(pointer, op, update, lowerReadMode, lowerWriteMode);
            if (readRequiresFence) {
                fence(readMode.getGlobalAccess());
            }
            if (writeRequiresFence) {
                fb.fence(writeMode.getGlobalAccess());
            }
        }
        return result;
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        if (isTailCallSafe()) {
            return super.tailCall(targetPtr, receiver, arguments);
        }
        // break tail call
        return super.return_(super.call(targetPtr, receiver, arguments));
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        // declare personality function
        MethodElement personalityMethod = UnwindExceptionStrategy.get(ctxt).getPersonalityMethod();
        Function function = ctxt.getExactFunction(personalityMethod);
        ctxt.getOrAddProgramModule(getRootElement()).declareFunction(function);
        return super.invokeNoReturn(targetPtr, receiver, arguments, catchLabel, targetArguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        // declare personality function
        MethodElement personalityMethod = UnwindExceptionStrategy.get(ctxt).getPersonalityMethod();
        Function function = ctxt.getExactFunction(personalityMethod);
        ctxt.getOrAddProgramModule(getRootElement()).declareFunction(function);
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    @Override
    public Value notNull(Value v) {
        return v;
    }

    private boolean isTailCallSafe() {
        if (getCurrentElement().hasAllModifiersOf(ClassFile.I_ACC_HIDDEN)) {
            Node callSite = getCallSite();
            while (callSite != null) {
                ExecutableElement element = callSite.getElement();
                if (!element.hasAllModifiersOf(ClassFile.I_ACC_HIDDEN)) {
                    return false;
                }
                callSite = callSite.getCallSite();
            }
            return true;
        }
        return false;
    }
}
