package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BinaryValue;
import cc.quarkus.qcc.graph.BooleanType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.CommutativeBinaryValue;
import cc.quarkus.qcc.graph.ConstantValue;
import cc.quarkus.qcc.graph.FieldReadValue;
import cc.quarkus.qcc.graph.FieldWrite;
import cc.quarkus.qcc.graph.FloatType;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.IfValue;
import cc.quarkus.qcc.graph.InitialMemoryState;
import cc.quarkus.qcc.graph.InstanceFieldWrite;
import cc.quarkus.qcc.graph.IntegerType;
import cc.quarkus.qcc.graph.InvocationValue;
import cc.quarkus.qcc.graph.MemoryState;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NonCommutativeBinaryValue;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiMemoryState;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.SignedIntegerType;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Throw;
import cc.quarkus.qcc.graph.TryInvocation;
import cc.quarkus.qcc.graph.TryInvocationValue;
import cc.quarkus.qcc.graph.TryThrow;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.UnaryValue;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.VoidType;
import cc.quarkus.qcc.graph.WordCastValue;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.LinkerInvoker;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import cc.quarkus.qcc.tool.llvm.LlcInvoker;
import cc.quarkus.qcc.tool.llvm.LlcTool;
import cc.quarkus.qcc.tool.llvm.LlcToolImpl;
import cc.quarkus.qcc.type.definition.DefinedMethodDefinition;
import cc.quarkus.qcc.type.definition.ResolvedMethodBody;
import cc.quarkus.qcc.type.definition.ResolvedMethodDefinition;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;
import io.smallrye.common.constraint.Assert;

final class LLVMNativeImageGenerator implements NativeImageGenerator {
    private final Module module = Module.newModule();
    private final Map<Type, Map<MethodIdentifier, FunctionDefinition>> functionsByType = new HashMap<>();
    private final ArrayDeque<ResolvedMethodDefinition> methodQueue = new ArrayDeque<>();
    private final Map<Type, cc.quarkus.qcc.machine.llvm.Value> types = new HashMap<>();
    private final LlcTool llc;
    private final CCompiler cc;

    LLVMNativeImageGenerator() {
        // fail fast if there's no context
        Context.requireCurrent();
        // todo: get config from context
        LLVMNativeImageGeneratorConfig config = new LLVMNativeImageGeneratorConfig() {
            public Optional<List<String>> entryPointClassNames() {
                return Optional.of(List.of("hello.world.Main"));
            }
        };

        // find llc
        // TODO: get target platform from config
        llc = ToolProvider.findAllTools(LlcToolImpl.class, Platform.HOST_PLATFORM, t -> true, LLVMNativeImageGeneratorFactory.class.getClassLoader()).iterator().next();
        // find C compiler
        cc = ToolProvider.findAllTools(CCompiler.class, Platform.HOST_PLATFORM, t -> true, LLVMNativeImageGeneratorFactory.class.getClassLoader()).iterator().next();
    }

    public void addEntryPoint(final DefinedMethodDefinition methodDefinition) {
        methodQueue.addLast(methodDefinition.resolve());
    }

    public void compile() {
        final Module module = this.module;
        while (! methodQueue.isEmpty()) {
            compileMethod(methodQueue.removeFirst());
        }
        // XXX print it to screen
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out))) {
            module.writeTo(bw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write out the object file
        final Path objectPath = Path.of("/tmp/build.o");
        final Path execPath = Path.of("/tmp/a.out");
        final LlcInvoker llcInv = llc.newInvoker();
        llcInv.setSource(InputSource.from(rw -> {
            try (BufferedWriter w = new BufferedWriter(rw)) {
                module.writeTo(w);
            }
        }, StandardCharsets.UTF_8));
        llcInv.setDestination(OutputDestination.of(objectPath));
        llcInv.setMessageHandler(ToolMessageHandler.REPORTING);
        try {
            llcInv.invoke();
        } catch (IOException e) {
            Context.error(null, "LLVM compilation failed: %s", e);
            return;
        }
        final LinkerInvoker ldInv = cc.newLinkerInvoker();
        ldInv.setOutputPath(execPath);
        ldInv.setMessageHandler(ToolMessageHandler.REPORTING);
        ldInv.addObjectFile(objectPath);
        try {
            ldInv.invoke();
        } catch (IOException e) {
            Context.error(null, "Linking failed: %s", e);
        }
        return;
    }

    FunctionDefinition getMethod(final ClassType ownerType, final MethodIdentifier identifier) {
        Map<MethodIdentifier, FunctionDefinition> typeMap = functionsByType.computeIfAbsent(ownerType, t -> new HashMap<>());
        FunctionDefinition def = typeMap.get(identifier);
        if (def != null) {
            return def;
        }
        StringBuilder b = new StringBuilder();
        b.append(".exact.");
        mangle(ownerType.getClassName(), b);
        b.append(".");
        mangle(identifier.getName(), b);
        b.append(".");
        mangle(identifier, b);
        def = module.define(b.toString());
        typeMap.put(identifier, def);
        return def;
    }

    void mangle(MethodTypeDescriptor desc, StringBuilder b) {
        mangle("(", b);
        int parameterCount = desc.getParameterCount();
        for (int i = 0; i < parameterCount; i ++) {
            // todo: not quite right...
            mangle(desc.getParameterType(i).toString(), b);
            b.append('_');
        }
        // todo: not quite right...
        mangle(desc.getReturnType().toString(), b);
    }

    void mangle(String str, StringBuilder b) {
        char ch;
        for (int i = 0; i < str.length(); i ++) {
            ch = str.charAt(i);
            if (ch == '(' || ch == ')') {
                b.append("__");
            } else if (ch == '.' || ch == '/') {
                b.append('_');
            } else if (ch == '_') {
                b.append("_1");
            } else if (ch == ';') {
                b.append("_2");
            } else if (ch == '[') {
                b.append("_3");
            } else if ('A' <= ch && ch <= 'Z') {
                b.append(ch);
            } else if ('a' <= ch && ch <= 'z') {
                b.append(ch);
            } else if ('0' <= ch && ch <= '9') {
                b.append(ch);
            } else {
                b.append("_0");
                b.append(hex(ch >>> 12));
                b.append(hex(ch >>> 8));
                b.append(hex(ch >>> 4));
                b.append(hex(ch));
            }
        }
    }

    char hex(int val) {
        val &= 0xf;
        if (val > 10) {
            return (char) ('a' + val - 10);
        } else {
            return (char) ('0' + val);
        }
    }

    void compileMethod(final ResolvedMethodDefinition definition) {
        ArrayDeque<ResolvedMethodDefinition> mq = methodQueue;
        Module module = this.module;
        final FunctionDefinition func = getMethod(definition.getEnclosingTypeDefinition().verify().getClassType(), definition.getMethodIdentifier()).callingConvention(CallingConvention.C).linkage(Linkage.EXTERNAL);
        int idx = 0;
        ResolvedMethodBody graph = definition.getMethodBody().verify().resolve();
        BasicBlock entryBlock = graph.getEntryBlock();
        Set<BasicBlock> reachableBlocks = entryBlock.calculateReachableBlocks();
        final MethodContext cache = new MethodContext(definition, func, reachableBlocks);
        func.returns(typeOf(definition.getReturnType()));
        final List<ParameterValue> paramVals = graph.getParameters();
        for (ParameterValue pv : paramVals) {
            cache.values.put(pv, func.param(typeOf(pv.getType())).name("p" + idx++).asValue());
        }
        cache.blocks.put(entryBlock, func);
        Schedule schedule = Schedule.forMethod(entryBlock);
        for (BasicBlock block : reachableBlocks) {
            build(cache, schedule, block.getTerminator());
        }
    }

    private Value typeOf(final Type type) {
        Value res = types.get(type);
        if (res != null) {
            return res;
        }
        if (type instanceof VoidType) {
            res = void_;
        } else if (type instanceof BooleanType) {
            res = i1;
        } else if (type instanceof IntegerType) {
            int bytes = ((IntegerType) type).getSize();
            if (bytes == 1) {
                res = i8;
            } else if (bytes == 2) {
                res = i16;
            } else if (bytes == 4) {
                res = i32;
            } else if (bytes == 8) {
                res = i64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof FloatType) {
            int bytes = ((FloatType) type).getSize();
            if (bytes == 4) {
                res = float32;
            } else if (bytes == 8) {
                res = float64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof ClassType) {
            // todo: lower class types to ref types at some earlier point
            res = ptrTo(i8);
        } else {
            throw new IllegalStateException();
        }
        types.put(type, res);
        return res;
    }

    private void build(final MethodContext cache, final Schedule schedule, final Node node) {
        if (node == null) {
            return;
        }
        if (! cache.built.add(node)) {
            // already built this one
            return;
        }
        // first build dependencies
        if (node instanceof cc.quarkus.qcc.graph.MemoryState) {
            MemoryState memoryState = (MemoryState) node;
            MemoryState dependency = memoryState.getMemoryDependency();
            build(cache, schedule, dependency);
        }
        int depCnt = node.getValueDependencyCount();
        for (int i = 0; i < depCnt; i ++) {
            build(cache, schedule, node.getValueDependency(i));
        }
        // all dependencies are now in the cache
        final cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(cache, schedule, schedule.getBlockForNode(node));
        if (node instanceof Terminator) {
            if (node instanceof ValueReturn) {
                cc.quarkus.qcc.graph.Value rv = ((ValueReturn) node).getReturnValue();
                target.ret(typeOf(rv.getType()), getValue(cache, rv));
            } else if (node instanceof Return) {
                target.ret();
            } else if (node instanceof TryInvocationValue) {
                TryInvocationValue tiv = (TryInvocationValue) node;
                throw new IllegalStateException();
            } else if (node instanceof TryInvocation) {
                TryInvocation ti = (TryInvocation) node;
                throw new IllegalStateException();
            } else if (node instanceof TryThrow) {
                TryThrow t = (TryThrow) node;
                target.br(getBlock(cache, schedule, t.getCatchHandler()));
            } else if (node instanceof Throw) {
                Throw t = (Throw) node;
                cc.quarkus.qcc.machine.llvm.Value doThrowFn = module.declare(".throw").asGlobal();
                target.call(void_, doThrowFn).arg(Types.i32, getValue(cache, t.getThrownValue()));
            } else if (node instanceof Goto) {
                BasicBlock jmpTarget = ((Goto) node).getNextBlock();
                target.br(getBlock(cache, schedule, jmpTarget));
            } else if (node instanceof If) {
                If ifInst = (If) node;
                cc.quarkus.qcc.graph.Value cond = ifInst.getCondition();
                BasicBlock tb = ifInst.getTrueBranch();
                BasicBlock fb = ifInst.getFalseBranch();
                cc.quarkus.qcc.machine.llvm.BasicBlock tTarget = getBlock(cache, schedule, tb);
                cc.quarkus.qcc.machine.llvm.BasicBlock fTarget = getBlock(cache, schedule, fb);
                cc.quarkus.qcc.machine.llvm.Value condVal = getValue(cache, cond);
                target.br(condVal, tTarget, fTarget);
            } else {
                throw new IllegalStateException();
            }
        } else if (node instanceof cc.quarkus.qcc.graph.Value) {
            cc.quarkus.qcc.graph.Value value = (cc.quarkus.qcc.graph.Value) node;
            cc.quarkus.qcc.machine.llvm.Value val;
            Value outputType = typeOf(value.getType());
            if (value instanceof BinaryValue) {
                BinaryValue binOp = (BinaryValue) value;
                Type javaInputType = binOp.getLeftInput().getType();
                Value inputType = typeOf(javaInputType);
                Value llvmLeft = getValue(cache, binOp.getLeftInput());
                Value llvmRight = getValue(cache, binOp.getRightInput());
                if (binOp instanceof CommutativeBinaryValue) {
                    CommutativeBinaryValue op = (CommutativeBinaryValue) binOp;
                    switch (op.getKind()) {
                        case ADD: val = isFloating(javaInputType) ?
                                        target.fadd(inputType, llvmLeft, llvmRight).asLocal() :
                                        target.add(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case AND: val = target.and(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case OR: val = target.or(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case XOR: val = target.xor(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case MULTIPLY: val = isFloating(javaInputType) ?
                                             target.fmul(inputType, llvmLeft, llvmRight).asLocal() :
                                             target.mul(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case CMP_EQ: val = isFloating(javaInputType) ?
                                           target.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).asLocal() :
                                           target.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal(); break;
                        case CMP_NE: val = isFloating(javaInputType) ?
                                           target.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).asLocal() :
                                           target.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).asLocal(); break;
                        default: throw new IllegalStateException();
                    }
                } else {
                    assert binOp instanceof NonCommutativeBinaryValue;
                    NonCommutativeBinaryValue op = (NonCommutativeBinaryValue) binOp;
                    switch (op.getKind()) {
                        case UNSIGNED_SHR: val = target.lshr(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case SHR: val = target.ashr(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case SHL: val = target.shl(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case SUB: val = isFloating(javaInputType) ?
                                        target.fsub(inputType, llvmLeft, llvmRight).asLocal() :
                                        target.sub(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case DIV: val = isFloating(javaInputType) ?
                                        target.fdiv(inputType, llvmLeft, llvmRight).asLocal() :
                                        isSigned(javaInputType) ?
                                        target.sdiv(inputType, llvmLeft, llvmRight).asLocal() :
                                        target.udiv(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case MOD: val = isFloating(javaInputType) ?
                                        target.frem(inputType, llvmLeft, llvmRight).asLocal() :
                                        isSigned(javaInputType) ?
                                        target.srem(inputType, llvmLeft, llvmRight).asLocal() :
                                        target.urem(inputType, llvmLeft, llvmRight).asLocal(); break;
                        case CMP_LT: val = isFloating(javaInputType) ?
                                           target.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).asLocal() :
                                           target.icmp(isSigned(javaInputType) ? IntCondition.slt : IntCondition.ult, inputType, llvmLeft, llvmRight).asLocal(); break;
                        case CMP_LE: val = isFloating(javaInputType) ?
                                           target.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).asLocal() :
                                           target.icmp(isSigned(javaInputType) ? IntCondition.sle : IntCondition.ule, inputType, llvmLeft, llvmRight).asLocal(); break;
                        case CMP_GT: val = isFloating(javaInputType) ?
                                           target.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).asLocal() :
                                           target.icmp(isSigned(javaInputType) ? IntCondition.sgt : IntCondition.ugt, inputType, llvmLeft, llvmRight).asLocal(); break;
                        case CMP_GE: val = isFloating(javaInputType) ?
                                           target.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).asLocal() :
                                           target.icmp(isSigned(javaInputType) ? IntCondition.sge : IntCondition.uge, inputType, llvmLeft, llvmRight).asLocal(); break;
                        default: throw new IllegalStateException();
                    }
                }
            } else if (value instanceof UnaryValue) {
                UnaryValue op = (UnaryValue) value;
                Type javaInputType = op.getInput().getType();
                Value inputType = typeOf(javaInputType);
                Value llvmInput = getValue(cache, op.getInput());
                switch (op.getKind()) {
                    case NEGATE: val = isFloating(javaInputType) ?
                                       target.fneg(inputType, llvmInput).asLocal() :
                                       target.sub(inputType, Values.ZERO, llvmInput).asLocal(); break;
                    default: throw new IllegalStateException();
                }
            } else if (value instanceof WordCastValue) {
                WordCastValue op = (WordCastValue) value;
                Type javaInputType = op.getInput().getType();
                Type javaOutputType = op.getType();
                Value inputType = typeOf(javaInputType);
                Value llvmInput = getValue(cache, op.getInput());
                switch (op.getKind()) {
                    case TRUNCATE: val = isFloating(javaInputType) ?
                                         target.ftrunc(inputType, llvmInput, outputType).asLocal() :
                                         target.trunc(inputType, llvmInput, outputType).asLocal(); break;
                    case EXTEND: val = isFloating(javaInputType) ?
                                       target.fpext(inputType, llvmInput, outputType).asLocal() :
                                       isSigned(javaInputType) ?
                                       target.sext(inputType, llvmInput, outputType).asLocal() :
                                       target.zext(inputType, llvmInput, outputType).asLocal(); break;
                    case BIT_CAST: val = target.bitcast(inputType, llvmInput, outputType).asLocal(); break;
                    case VALUE_CONVERT: val = isFloating(javaInputType) ?
                                              isSigned(javaOutputType) ?
                                              target.fptosi(inputType, llvmInput, outputType).asLocal() :
                                              target.fptoui(inputType, llvmInput, outputType).asLocal() :
                                              isSigned(javaInputType) ?
                                              target.sitofp(inputType, llvmInput, outputType).asLocal() :
                                              target.uitofp(inputType, llvmInput, outputType).asLocal(); break;
                    default: throw new IllegalStateException();
                }
            } else if (value instanceof FieldReadValue) {
                throw new IllegalStateException();
            } else if (value instanceof IfValue) {
                IfValue op = (IfValue) value;
                cc.quarkus.qcc.graph.Value trueValue = op.getTrueValue();
                Value inputType = typeOf(trueValue.getType());
                val = target.select(typeOf(op.getCond().getType()), getValue(cache, op.getCond()), inputType, getValue(cache, trueValue), getValue(cache, op.getFalseValue())).asLocal();
            } else if (value instanceof PhiValue) {
                PhiValue phiValue = (PhiValue) value;
                Phi phi = target.phi(outputType);
                val = phi.asLocal();
                for (BasicBlock knownBlock : cache.knownBlocks) {
                    cc.quarkus.qcc.graph.Value v = phiValue.getValueForBlock(knownBlock);
                    build(cache, schedule, v);
                    if (v != null) {
                        phi.item(getValue(cache, v), getBlock(cache, schedule, knownBlock));
                    }
                }
            } else if (value instanceof InvocationValue) {
                InvocationValue inv = (InvocationValue) value;
                Type returnType = inv.getInvocationTarget().getReturnType();
                Call call = target.call(typeOf(returnType), getFunctionOf(cache, inv.getMethodOwner(), inv.getInvocationTarget()));
                int cnt = inv.getArgumentCount();
                for (int i = 0; i < cnt; i++) {
                    cc.quarkus.qcc.graph.Value arg = inv.getArgument(i);
                    call.arg(typeOf(arg.getType()), getValue(cache, arg));
                }
                val = call.asLocal();
            } else if (value instanceof ParameterValue || value instanceof ConstantValue) {
                // already cached
                return;
            } else {
                throw new IllegalStateException();
            }
            cache.values.put(value, val);
        } else if (node instanceof MemoryState) {
            MemoryState memoryState = (MemoryState) node;
            if (memoryState instanceof InstanceFieldWrite) {
                InstanceFieldWrite ifw = (InstanceFieldWrite) memoryState;
                target.store(Types.i32, getValue(cache, ifw.getWriteValue()), Types.i32, Values.ZERO /* todo: get address of static field */);
            } else if (memoryState instanceof FieldWrite) {
                // static
                FieldWrite sfw = (FieldWrite) memoryState;
                target.store(Types.i32, getValue(cache, sfw.getWriteValue()), Types.i32, Values.ZERO /* todo: get address of static field */);
            } else if (memoryState instanceof InitialMemoryState) {
                // no action
            } else if (memoryState instanceof PhiMemoryState) {
                // no action
            } else {
                throw new IllegalStateException();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private Value getValue(final MethodContext cache, final cc.quarkus.qcc.graph.Value input) {
        if (input instanceof ConstantValue) {
            if (input.getType() instanceof IntegerType) {
                // todo: 128 bit integer types, handle signedness
                return Values.intConstant(((ConstantValue) input).longValue());
            } else {
                // todo: floating point constants
                throw new IllegalStateException();
            }
        } else {
            Value value = cache.values.get(input);
            if (value == null) {
                throw new IllegalStateException("No value for input " + input);
            }
            return value;
        }
    }

    private boolean isFloating(final Type inputType) {
        return inputType instanceof FloatType;
    }

    private boolean isSigned(final Type inputType) {
        return inputType instanceof SignedIntegerType;
    }

    private Value getFunctionOf(final MethodContext cache, final ClassType owner, final MethodIdentifier invocationTarget) {
        final ResolvedMethodDefinition resolved = owner.getDefinition().resolve().resolveMethod(invocationTarget);
        compileMethod(resolved);
        return getMethod(owner, invocationTarget).asGlobal();
    }

    private cc.quarkus.qcc.machine.llvm.BasicBlock getBlock(final MethodContext cache, final Schedule schedule, final BasicBlock bb) {
        cc.quarkus.qcc.machine.llvm.BasicBlock target = cache.blocks.get(bb);
        if (target == null) {
            target = cache.def.createBlock();
            cache.blocks.put(bb, target);
        }
        return target;
    }

    static final class MethodContext {
        final ResolvedMethodDefinition currentMethod;
        final FunctionDefinition def;
        final Map<cc.quarkus.qcc.graph.Value, cc.quarkus.qcc.machine.llvm.Value> values = new HashMap<>();
        final Map<cc.quarkus.qcc.graph.BasicBlock, cc.quarkus.qcc.machine.llvm.BasicBlock> blocks = new HashMap<>();
        final Set<cc.quarkus.qcc.graph.BasicBlock> processed = new HashSet<>();
        final Set<cc.quarkus.qcc.graph.BasicBlock> knownBlocks;
        final Set<Node> built = new HashSet<>();

        MethodContext(final ResolvedMethodDefinition currentMethod, final FunctionDefinition def, final Set<BasicBlock> knownBlocks) {
            this.currentMethod = currentMethod;
            this.def = def;
            this.knownBlocks = knownBlocks;
        }
    }
}
