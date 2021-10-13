package org.qbicc.plugin.conversion;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.unwind.UnwindHelper;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NumericType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

public class LLVMCompatibleBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public LLVMCompatibleBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
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
        FunctionDeclaration declaration = ctxt.getImplicitSection(getRootElement()).declareFunction(null, funcName, functionType);
        return getFirstBuilder().callNoSideEffects(functionOf(declaration), List.of(v1, v2));
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
        FunctionDeclaration declaration = ctxt.getImplicitSection(getRootElement()).declareFunction(null, functionName, functionType);
        return getFirstBuilder().callNoSideEffects(functionOf(declaration), List.of(v));
    }

    @Override
    public Value bitReverse(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType, inputType);
        int minBits = inputType.getMinBits();
        String functionName = "llvm.bitreverse.i" + minBits;
        FunctionDeclaration declaration = ctxt.getImplicitSection(getRootElement()).declareFunction(null, functionName, functionType);
        return getFirstBuilder().callNoSideEffects(functionOf(declaration), List.of(v));
    }

    @Override
    public Value countLeadingZeros(Value v) {
        TypeSystem tps = ctxt.getTypeSystem();
        IntegerType inputType = (IntegerType) v.getType();
        FunctionType functionType = tps.getFunctionType(inputType.asUnsigned(), inputType, tps.getBooleanType());
        int minBits = inputType.getMinBits();
        String functionName = "llvm.ctlz.i" + minBits;
        FunctionDeclaration declaration = ctxt.getImplicitSection(getRootElement()).declareFunction(null, functionName, functionType);
        Value result = getFirstBuilder().callNoSideEffects(functionOf(declaration), List.of(v, ctxt.getLiteralFactory().literalOf(false)));
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
        FunctionDeclaration declaration = ctxt.getImplicitSection(getRootElement()).declareFunction(null, functionName, functionType);
        Value result = getFirstBuilder().callNoSideEffects(functionOf(declaration), List.of(v, ctxt.getLiteralFactory().literalOf(false)));
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
        FunctionDeclaration declaration = ctxt.getImplicitSection(getRootElement()).declareFunction(null, functionName, functionType);
        Value result = getFirstBuilder().callNoSideEffects(functionOf(declaration), List.of(v));
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
    public Value extractElement(Value array, Value index) {
        if (!(index instanceof Literal)) {
            ctxt.error(getLocation(), "Index of ExtractElement must be constant");
        }
        return super.extractElement(array, index);
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
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.VOLATILE) {
            Value loaded = super.load(handle, MemoryAtomicityMode.ACQUIRE);
            fence(MemoryAtomicityMode.ACQUIRE);
            return loaded;
        } else {
            return super.load(handle, mode);
        }
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        if (handle.getValueType() instanceof BooleanType) {
            ctxt.error("Invalid boolean-typed handle %s", handle);
        }
        if (value.getType() instanceof BooleanType) {
            ctxt.error("Invalid boolean-typed value %s", value);
        }
        if (mode == MemoryAtomicityMode.VOLATILE) {
            fence(MemoryAtomicityMode.RELEASE);
            return super.store(handle, value, MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
        } else {
            return super.store(handle, value, mode);
        }
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode, CmpAndSwap.Strength strength) {
        boolean volatileSuccess = false, volatileFailure = false;
        if (successMode == MemoryAtomicityMode.VOLATILE) {
            volatileSuccess = true;
            successMode = MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT;
        }
        if (failureMode == MemoryAtomicityMode.VOLATILE) {
            volatileFailure = true;
            failureMode = MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT;
        }
        if (successMode == MemoryAtomicityMode.NONE || successMode == MemoryAtomicityMode.UNORDERED) {
            successMode = MemoryAtomicityMode.MONOTONIC;
        }
        if (failureMode == MemoryAtomicityMode.NONE || failureMode == MemoryAtomicityMode.UNORDERED) {
            failureMode = MemoryAtomicityMode.MONOTONIC;
        }
        Value result = super.cmpAndSwap(target, expect, update, successMode, failureMode, strength);
        if (volatileSuccess) {
            if (volatileFailure) {
                fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            } else {
                // todo: only fence on success
                fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            }
        } else {
            // todo: only fence on error
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
        }
        return result;
    }

    @Override
    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndAdd(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.add(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndAdd(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndBitwiseAnd(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.and(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndBitwiseAnd(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndBitwiseOr(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.or(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndBitwiseOr(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndBitwiseXor(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.xor(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndBitwiseXor(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndSet(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.load(target, atomicityMode);
            super.store(target, update, atomicityMode);
            return result;
        } else {
            return super.getAndSet(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndSetMax(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.max(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndSetMax(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndSetMin(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.min(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndSetMin(target, update, atomicityMode);
        }
    }

    @Override
    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        if (atomicityMode == MemoryAtomicityMode.VOLATILE) {
            Value result = super.getAndSub(target, update, MemoryAtomicityMode.ACQUIRE_RELEASE);
            fence(MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            return result;
        } else if (atomicityMode == MemoryAtomicityMode.UNORDERED || atomicityMode == MemoryAtomicityMode.NONE) {
            Value result = super.sub(super.load(target, atomicityMode), update);
            super.store(target, result, atomicityMode);
            return result;
        } else {
            return super.getAndSub(target, update, atomicityMode);
        }
    }

    @Override
    public Node classInitCheck(ObjectType objectType) {
        // either this is handled by an earlier BBB, or else init was 100% build time
        return nop();
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        // todo: we can support "real" tail calls in certain situations
        // break tail call
        Value retVal = super.call(target, arguments);
        if (isVoidFunction(target)) {
            return super.return_();
        } else {
            return super.return_(retVal);
        }
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        // declare personality function
        MethodElement personalityMethod = UnwindHelper.get(ctxt).getPersonalityMethod();
        ctxt.getImplicitSection(getRootElement()).declareFunction(null, personalityMethod.getName(), personalityMethod.getType());
        return super.invokeNoReturn(target, arguments, catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        // declare personality function
        MethodElement personalityMethod = UnwindHelper.get(ctxt).getPersonalityMethod();
        ctxt.getImplicitSection(getRootElement()).declareFunction(null, personalityMethod.getName(), personalityMethod.getType());
        return super.invoke(target, arguments, catchLabel, resumeLabel);
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        // declare personality function
        MethodElement personalityMethod = UnwindHelper.get(ctxt).getPersonalityMethod();
        ctxt.getImplicitSection(getRootElement()).declareFunction(null, personalityMethod.getName(), personalityMethod.getType());
        // todo: we can support "real" tail calls in certain situations
        // break tail invoke
        BlockLabel resumeLabel = new BlockLabel();
        Value retVal = super.invoke(target, arguments, catchLabel, resumeLabel);
        begin(resumeLabel);
        if (isVoidFunction(target)) {
            return super.return_();
        } else {
            return super.return_(retVal);
        }
    }

    private static boolean isVoidFunction(ValueHandle target) {
        FunctionType type = (FunctionType) target.getValueType();
        return type.getReturnType() instanceof VoidType;
    }
}
