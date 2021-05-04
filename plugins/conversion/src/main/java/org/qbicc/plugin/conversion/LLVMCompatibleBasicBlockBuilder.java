package org.qbicc.plugin.conversion;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Triable;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Function;
import org.qbicc.plugin.unwind.UnwindHelper;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NumericType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;

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
            numericType = (v1.getType().getSize() == 4) ? tps.getFloat32Type() : tps.getFloat64Type();
            fullFuncName = "llvm." + funcName + "imum.f" + numericType.getMinBits();
            return minMaxIntrinsic(fullFuncName, numericType, v1, v2);
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
        SymbolLiteral functionSymbol = ctxt.getLiteralFactory().literalOfSymbol(funcName, functionType);
        ctxt.getImplicitSection(getCurrentElement()).declareFunction(null, funcName, functionType);
        return getFirstBuilder().callFunction(functionSymbol, List.of(v1, v2));
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
        if (mode == MemoryAtomicityMode.VOLATILE) {
            fence(MemoryAtomicityMode.RELEASE);
            return super.store(handle, value, MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
        } else {
            return super.store(handle, value, mode);
        }
    }

    @Override
    public BasicBlock try_(final Triable operation, final BlockLabel resumeLabel, final BlockLabel exceptionHandler) {
        Function personalityFunction = ctxt.getExactFunction(UnwindHelper.get(ctxt).getPersonalityMethod());
        ctxt.getImplicitSection(getCurrentElement()).declareFunction(null, personalityFunction.getName(), personalityFunction.getType());
        return super.try_(operation, resumeLabel, exceptionHandler);
    }
}
