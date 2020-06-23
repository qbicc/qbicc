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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.BasicBlock;
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
import cc.quarkus.qcc.graph.InstanceFieldWrite;
import cc.quarkus.qcc.graph.IntegerType;
import cc.quarkus.qcc.graph.InvocationValue;
import cc.quarkus.qcc.graph.MemoryState;
import cc.quarkus.qcc.graph.NonCommutativeBinaryValue;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.ProgramNode;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Throw;
import cc.quarkus.qcc.graph.TryInvocation;
import cc.quarkus.qcc.graph.TryInvocationValue;
import cc.quarkus.qcc.graph.TryThrow;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
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
        final MethodContext cache = new MethodContext(func, reachableBlocks);
        func.returns(typeOf(definition.getReturnType()));
        final List<ParameterValue> paramVals = graph.getParameters();
        for (ParameterValue pv : paramVals) {
            cache.values.put(pv, func.param(typeOf(pv.getType())).name("p" + idx++).asValue());
        }
        cache.blocks.put(entryBlock, func);
        // write the terminal instructions
        for (BasicBlock bb : reachableBlocks) {
            addTermInst(cache, bb.getTerminator(), getBlock(cache, bb));
        }
    }

    private Value typeOf(final Type type) {
        Value res = types.get(type);
        if (res != null) {
            return res;
        }
        if (type instanceof BooleanType) {
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

    private void addTermInst(final MethodContext cache, final Terminator inst, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        MemoryState memoryDependency = inst.getMemoryDependency();
        if (memoryDependency != null) {
            process(cache, memoryDependency);
        }
        if (inst instanceof ValueReturn) {
            cc.quarkus.qcc.graph.Value rv = ((ValueReturn) inst).getReturnValue();
            target.ret(typeOf(rv.getType()), getValue(cache, rv));
        } else if (inst instanceof Return) {
            target.ret();
        } else if (inst instanceof TryInvocationValue) {
            TryInvocationValue tiv = (TryInvocationValue) inst;
            throw new IllegalStateException();

        } else if (inst instanceof TryInvocation) {
            TryInvocation ti = (TryInvocation) inst;
            throw new IllegalStateException();

        } else if (inst instanceof TryThrow) {
            TryThrow t = (TryThrow) inst;
            target.br(getBlock(cache, t.getCatchHandler()));
        } else if (inst instanceof Throw) {
            Throw t = (Throw) inst;
            cc.quarkus.qcc.machine.llvm.Value doThrowFn = module.declare(".throw").asGlobal();
            target.call(Types.i32, doThrowFn).arg(Types.i32, getValue(cache, t.getThrownValue()));
        } else if (inst instanceof Goto) {
            BasicBlock jmpTarget = ((Goto) inst).getNextBlock();
            target.br(getBlock(cache, jmpTarget));
        } else if (inst instanceof If) {
            If ifInst = (If) inst;
            cc.quarkus.qcc.graph.Value cond = ifInst.getCondition();
            BasicBlock tb = ifInst.getTrueBranch();
            BasicBlock fb = ifInst.getFalseBranch();
            cc.quarkus.qcc.machine.llvm.BasicBlock tTarget = getBlock(cache, tb);
            cc.quarkus.qcc.machine.llvm.BasicBlock fTarget = getBlock(cache, fb);
            cc.quarkus.qcc.machine.llvm.Value condVal = getValue(cache, cond);
            target.br(condVal, tTarget, fTarget);
        } else {
            throw new IllegalStateException();
        }
    }

    private void process(final MethodContext cache, final MemoryState memoryState) {
        if (memoryState instanceof cc.quarkus.qcc.graph.Value) {
            getValue(cache, (cc.quarkus.qcc.graph.Value) memoryState);
        } else if (memoryState instanceof Terminator) {
            throw new IllegalStateException();
        } else if (memoryState instanceof InstanceFieldWrite) {
            InstanceFieldWrite ifw = (InstanceFieldWrite) memoryState;
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(cache, ifw.getOwner());
            target.store(Types.i32, getValue(cache, ifw.getWriteValue()), Types.i32, Values.ZERO /* todo: get address of static field */);
        } else if (memoryState instanceof FieldWrite) {
            // static
            FieldWrite sfw = (FieldWrite) memoryState;
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(cache, sfw.getOwner());
            target.store(Types.i32, getValue(cache, sfw.getWriteValue()), Types.i32, Values.ZERO /* todo: get address of static field */);
        } else {
            throw new IllegalStateException();
        }
    }


    private cc.quarkus.qcc.machine.llvm.Value getValue(final MethodContext cache, final cc.quarkus.qcc.graph.Value value) {
        cc.quarkus.qcc.machine.llvm.Value val = cache.values.get(value);
        if (val != null) {
            return val;
        }
        if (value instanceof MemoryState) {
            MemoryState memoryDependency = ((MemoryState) value).getMemoryDependency();
            if (memoryDependency != null) {
                process(cache, memoryDependency);
            }
        }
        Value outputType = typeOf(value.getType());
        if (value instanceof ProgramNode) {
            BasicBlock owner = ((ProgramNode) value).getOwner();
            final cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(cache, owner);
            if (value instanceof CommutativeBinaryValue) {
                CommutativeBinaryValue op = (CommutativeBinaryValue) value;
                Value inputType = typeOf(op.getLeftInput().getType());
                switch (op.getKind()) {
                    case ADD: val = target.add(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case AND: val = target.and(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case OR: val = target.or(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case XOR: val = target.xor(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case MULTIPLY: val = target.mul(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case CMP_EQ: val = target.icmp(IntCondition.eq, inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case CMP_NE: val = target.icmp(IntCondition.ne, inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    default: throw new IllegalStateException();
                }
                cache.values.put(value, val);
            } else if (value instanceof NonCommutativeBinaryValue) {
                NonCommutativeBinaryValue op = (NonCommutativeBinaryValue) value;
                Value inputType = typeOf(op.getLeftInput().getType());
                switch (op.getKind()) {
                    case SUB: val = target.sub(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case DIV: val = target.sdiv(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case MOD: val = target.srem(inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case CMP_LT: val = target.icmp(IntCondition.slt, inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case CMP_LE: val = target.icmp(IntCondition.sle, inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case CMP_GT: val = target.icmp(IntCondition.sgt, inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    case CMP_GE: val = target.icmp(IntCondition.sge, inputType, getValue(cache, op.getLeftInput()), getValue(cache, op.getRightInput())).asLocal(); break;
                    default: throw new IllegalStateException();
                }
                cache.values.put(value, val);
            } else if (value instanceof FieldReadValue) {
                throw new IllegalStateException();
            } else if (value instanceof IfValue) {
                IfValue op = (IfValue) value;
                cc.quarkus.qcc.graph.Value trueValue = op.getTrueValue();
                Value inputType = typeOf(trueValue.getType());
                val = target.select(Types.i1, getValue(cache, op.getCond()), inputType, getValue(cache, trueValue), getValue(cache, op.getFalseValue())).asLocal();
                cache.values.put(value, val);
            } else if (value instanceof PhiValue) {
                PhiValue phiValue = (PhiValue) value;
                if (true) {
                    final Iterator<BasicBlock> iterator = cache.knownBlocks.iterator();
                    while (iterator.hasNext()) {
                        BasicBlock b1 = iterator.next();
                        cc.quarkus.qcc.graph.Value v1 = phiValue.getValueForBlock(b1);
                        if (v1 != null) {
                            // got first value
                            while (iterator.hasNext()) {
                                BasicBlock b2 = iterator.next();
                                cc.quarkus.qcc.graph.Value v2 = phiValue.getValueForBlock(b2);
                                if (v2 != null && v2 != v1) {
                                    // it's a phi, so we'll just live with it
                                    Phi phi = target.phi(outputType);
                                    cache.values.put(value, val = phi.asLocal());
                                    phi.item(getValue(cache, v1), getBlock(cache, b1));
                                    phi.item(getValue(cache, v2), getBlock(cache, b2));
                                    while (iterator.hasNext()) {
                                        b2 = iterator.next();
                                        v2 = phiValue.getValueForBlock(b2);
                                        if (v2 != null) {
                                            phi.item(getValue(cache, v2), getBlock(cache, b2));
                                        }
                                    }
                                    return val;
                                }
                            }
                            // only one value for phi!
                            phiValue.replaceWith(v1);
                            return getValue(cache, v1);
                        }
                    }
                } else {
                    Phi phi = target.phi(outputType);
                    cache.values.put(value, val = phi.asLocal());
                    for (BasicBlock knownBlock : cache.knownBlocks) {
                        cc.quarkus.qcc.graph.Value v = phiValue.getValueForBlock(knownBlock);
                        if (v != null) {
                            phi.item(getValue(cache, v), getBlock(cache, knownBlock));
                        }
                    }
                    return val;
                }
                // no branches!
                throw new IllegalStateException();
            } else if (value instanceof InvocationValue) {
                InvocationValue inv = (InvocationValue) value;
                Type returnType = inv.getInvocationTarget().getReturnType();
                Call call = target.call(typeOf(returnType), getFunctionOf(cache, inv.getMethodOwner(), inv.getInvocationTarget()));
                int cnt = inv.getArgumentCount();
                for (int i = 0; i < cnt; i ++) {
                    cc.quarkus.qcc.graph.Value arg = inv.getArgument(i);
                    call.arg(typeOf(arg.getType()), getValue(cache, arg));
                }
                val = call.asLocal();
            } else {
                throw new IllegalStateException();
            }
        } else if (value instanceof ConstantValue) {
            if (value.getType() instanceof IntegerType) {
                if (((IntegerType) value.getType()).getSize() > 4) {
                    cache.values.put(value, val = LLVM.intConstant(((ConstantValue) value).longValue()));
                } else {
                    cache.values.put(value, val = LLVM.intConstant(((ConstantValue) value).intValue()));
                }
            } else if (value.getType() instanceof BooleanType) {
                if (((ConstantValue) value).isFalse()) {
                    cache.values.put(value, val = Values.FALSE);
                } else {
                    cache.values.put(value, val = Values.TRUE);
                }
            } else {
                throw new IllegalStateException();
            }
        } else {
            throw new IllegalStateException();
        }
        return val;
    }

    private Value getFunctionOf(final MethodContext cache, final ClassType owner, final MethodIdentifier invocationTarget) {
        return getMethod(owner, invocationTarget).asGlobal();
    }

    private static cc.quarkus.qcc.machine.llvm.BasicBlock getBlock(final MethodContext cache, final BasicBlock bb) {
        cc.quarkus.qcc.machine.llvm.BasicBlock target = cache.blocks.get(bb);
        if (target == null) {
            target = cache.def.createBlock();
            cache.blocks.put(bb, target);
        }
        return target;
    }

    static final class MethodContext {
        final FunctionDefinition def;
        final Map<cc.quarkus.qcc.graph.Value, cc.quarkus.qcc.machine.llvm.Value> values = new HashMap<>();
        final Map<cc.quarkus.qcc.graph.BasicBlock, cc.quarkus.qcc.machine.llvm.BasicBlock> blocks = new HashMap<>();
        final Set<cc.quarkus.qcc.graph.BasicBlock> processed = new HashSet<>();
        final Set<cc.quarkus.qcc.graph.BasicBlock> knownBlocks;

        MethodContext(final FunctionDefinition def, final Set<BasicBlock> knownBlocks) {
            this.def = def;
            this.knownBlocks = knownBlocks;
        }
    }
}
