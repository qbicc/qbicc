package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.context.AnalyticPhaseContext;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.graph.Catch;
import cc.quarkus.qcc.graph.CmpEq;
import cc.quarkus.qcc.graph.CmpGe;
import cc.quarkus.qcc.graph.CmpGt;
import cc.quarkus.qcc.graph.CmpLe;
import cc.quarkus.qcc.graph.CmpLt;
import cc.quarkus.qcc.graph.CmpNe;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.Select;
import cc.quarkus.qcc.graph.InstanceInvocationValue;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.StaticInvocationValue;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.TerminatorVisitor;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.LinkerInvoker;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import cc.quarkus.qcc.tool.llvm.LlcInvoker;
import cc.quarkus.qcc.tool.llvm.LlcTool;
import cc.quarkus.qcc.tool.llvm.LlcToolImpl;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.ResolvedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import io.smallrye.common.constraint.Assert;

final class LLVMNativeImageGenerator implements NativeImageGenerator {
    private final Module module = Module.newModule();
    private final Map<ResolvedTypeDefinition, Map<ParameterizedExecutableElement, FunctionDefinition>> functionsByType = new HashMap<>();
    private final ArrayDeque<MethodElement> methodQueue = new ArrayDeque<>();
    private final Map<Type, cc.quarkus.qcc.machine.llvm.Value> types = new HashMap<>();
    private final LlcTool llc;
    private final CCompiler cc;
    private final ObjectFileProvider objProvider;
    private final AnalyticPhaseContext context;

    LLVMNativeImageGenerator(final AnalyticPhaseContext context) {
        this.context = context;
        // todo: get config from context
        LLVMNativeImageGeneratorConfig config = new LLVMNativeImageGeneratorConfig() {
            public Optional<List<String>> entryPointClassNames() {
                return Optional.of(List.of("hello.world.Main"));
            }
        };

        // find llc
        // TODO: use class loader from context
        ClassLoader classLoader = LLVMNativeImageGenerator.class.getClassLoader();
        // TODO: get target platform from config
        llc = ToolProvider.findAllTools(LlcToolImpl.class, Platform.HOST_PLATFORM, t -> true, classLoader).iterator().next();
        // find C compiler
        cc = ToolProvider.findAllTools(CCompiler.class, Platform.HOST_PLATFORM, t -> true, classLoader).iterator().next();
        objProvider = ObjectFileProvider.findProvider(Platform.HOST_PLATFORM.getObjectType(), classLoader).orElseThrow();
    }

    public void addEntryPoint(final MethodElement methodDefinition) {
        methodQueue.addLast(methodDefinition);
    }

    public void compile() {
        // ▪ compile class and interface mapping

        // ▪ establish object layouts

        // ▪ collection for object files
        List<Path> objects = new ArrayList<>();
        // ▪ write out the native image object code
        ProgramWriter programWriter = new ProgramWriter();
        // XXX move this writing code to program writer
        final Module module = this.module;
        while (! methodQueue.isEmpty()) {
            compileMethod(methodQueue.removeFirst(), false);
        }
        // XXX print it to screen
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out))) {
            module.writeTo(bw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Path programPath = Path.of("/tmp/build.o");
        objects.add(programPath);
        final LlcInvoker llcInv = llc.newInvoker();
        llcInv.setSource(InputSource.from(rw -> {
            try (BufferedWriter w = new BufferedWriter(rw)) {
                module.writeTo(w);
            }
        }, StandardCharsets.UTF_8));
        llcInv.setDestination(OutputDestination.of(programPath));
        llcInv.setMessageHandler(ToolMessageHandler.reporting(context));
        try {
            llcInv.invoke();
        } catch (IOException e) {
            context.error("LLVM compilation failed: %s", e);
            return;
        }
        // ▪ analyze the written object file
        try (ObjectFile program = objProvider.openObjectFile(programPath)) {

        } catch (IOException e) {
            context.error("Failed to read object file \"%s\": %s", programPath, e);
            return;
        }
        // XXX
        // ▪ write reachable part of stored heap to object file
        // XXX
        // ▪ write IP-to-program mapping to object file
        // XXX
        // ▪ write GC stack maps to object file
        // XXX
        // ▪ link it all together
        final Path execPath = Path.of("/tmp/a.out");
        final LinkerInvoker ldInv = cc.newLinkerInvoker();
        ldInv.setOutputPath(execPath);
        ldInv.setMessageHandler(ToolMessageHandler.reporting(context));
        ldInv.addObjectFiles(objects);
        try {
            ldInv.invoke();
        } catch (IOException e) {
            context.error("Linking failed: %s", e);
        }
        return;
    }

    FunctionDefinition getMethod(final ResolvedTypeDefinition ownerType, final ParameterizedExecutableElement element) {
        Map<ParameterizedExecutableElement, FunctionDefinition> typeMap = functionsByType.computeIfAbsent(ownerType, t -> new HashMap<>());
        FunctionDefinition def = typeMap.get(element);
        if (def != null) {
            return def;
        }
        StringBuilder b = new StringBuilder();
        b.append(".exact.");
        mangle(ownerType.getInternalName(), b);
        b.append(".");
        if (element instanceof MethodElement) {
            mangle(((MethodElement) element).getName(), b);
        } else {
            mangle("<init>", b);
        }
        b.append(".");
        mangle(element, b);
        def = module.define(b.toString());
        typeMap.put(element, def);
        return def;
    }

    void mangle(ParameterizedExecutableElement desc, StringBuilder b) {
        mangle("(", b);
        int parameterCount = desc.getParameterCount();
        for (int i = 0; i < parameterCount; i ++) {
            // todo: not quite right...
            mangle(desc.getParameter(i).getType().toString(), b);
            b.append('_');
        }
        if (desc instanceof MethodElement) {
            // todo: not quite right...
            mangle(((MethodElement) desc).getReturnType().toString(), b);
        }
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

    void compileMethod(final ParameterizedExecutableElement definition, final boolean virtual) {
        ArrayDeque<MethodElement> mq = methodQueue;
        Module module = this.module;
        final FunctionDefinition func = getMethod(definition.getEnclosingType().validate().resolve(), definition).callingConvention(CallingConvention.C).linkage(Linkage.EXTERNAL);
        int idx = 0;
        if (virtual) {
            throw new UnsupportedOperationException("Virtual dispatch");
        }
        MethodHandle methodHandle = definition.getMethodBody();
        if (methodHandle == null) {
            // nothing to do
            return;
        }
        MethodBody methodBody = methodHandle.createMethodBody();
        BasicBlock entryBlock = methodBody.getEntryBlock();
        Set<BasicBlock> reachableBlocks = entryBlock.calculateReachableBlocks();
        Schedule schedule = Schedule.forMethod(entryBlock);
        final MethodContext cache = new MethodContext(definition, func, reachableBlocks, schedule);
        if (definition instanceof MethodDescriptor) {
            func.returns(typeOf(((MethodDescriptor) definition).getReturnType()));
        } else {
            func.returns(void_);
        }
        int cnt = methodBody.getParameterCount();
        for (int i = 0; i < cnt; i ++) {
            cache.values.put(methodBody.getParameterValue(i), func.param(typeOf(definition.getParameter(i).getType())).name("p" + idx++).asValue());
        }
        cache.blocks.put(entryBlock, func);
        for (BasicBlock block : reachableBlocks) {
            block.getTerminator().accept(terminatorVisitor, cache);
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
            int bytes = (int) ((IntegerType) type).getSize();
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
            int bytes = (int) ((FloatType) type).getSize();
            if (bytes == 4) {
                res = float32;
            } else if (bytes == 8) {
                res = float64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof ReferenceType) {
            // todo: lower class types to ref types at some earlier point
            res = ptrTo(i8);
        } else {
            throw new IllegalStateException();
        }
        types.put(type, res);
        return res;
    }

    private final ValueVisitor<MethodContext, Value> valueVisitor = new ValueVisitor<MethodContext, Value>() {
        public Value visitUnknown(final MethodContext param, final cc.quarkus.qcc.graph.Value node) {
            throw new IllegalStateException();
        }

        public Value visit(final MethodContext param, final Add node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fadd(inputType, llvmLeft, llvmRight).asLocal() :
                          target.add(inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final And node) {
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            Value val = getBlock(param, node).and(typeOf(node.getType()), llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Or node) {
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            Value val = getBlock(param, node).or(typeOf(node.getType()), llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Xor node) {
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            Value val = getBlock(param, node).xor(typeOf(node.getType()), llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Multiply node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fmul(inputType, llvmLeft, llvmRight).asLocal() :
                          target.mul(inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final CmpEq node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).asLocal() :
                          target.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final CmpNe node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).asLocal() :
                          target.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final CmpLt node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).asLocal() :
                        isSigned(node.getType()) ?
                          target.icmp(IntCondition.slt, inputType, llvmLeft, llvmRight).asLocal() :
                          target.icmp(IntCondition.ult, inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final CmpLe node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).asLocal() :
                        isSigned(node.getType()) ?
                          target.icmp(IntCondition.sle, inputType, llvmLeft, llvmRight).asLocal() :
                          target.icmp(IntCondition.ule, inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final CmpGt node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).asLocal() :
                        isSigned(node.getType()) ?
                          target.icmp(IntCondition.sgt, inputType, llvmLeft, llvmRight).asLocal() :
                          target.icmp(IntCondition.ugt, inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final CmpGe node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).asLocal() :
                        isSigned(node.getType()) ?
                          target.icmp(IntCondition.sge, inputType, llvmLeft, llvmRight).asLocal() :
                          target.icmp(IntCondition.uge, inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Select node) {
            cc.quarkus.qcc.graph.Value trueValue = node.getTrueValue();
            Value inputType = typeOf(trueValue.getType());
            cc.quarkus.qcc.graph.Value falseValue = node.getFalseValue();
            Value val = getBlock(param, node).select(typeOf(node.getCondition().getType()), getValue(param, node.getCondition()), inputType, getValue(param, trueValue), getValue(param, falseValue)).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Catch node) {
            // todo: landingpad
            return null;
        }

        public Value visit(final MethodContext param, final PhiValue node) {
            Phi phi = getBlock(param, node).phi(typeOf(node.getType()));
            param.values.put(node, phi.asLocal());
            for (BasicBlock knownBlock : param.knownBlocks) {
                cc.quarkus.qcc.graph.Value v = node.getValueForBlock(knownBlock);
                if (v != null) {
                    // process dependencies
                    v.accept(this, param);
                    phi.item(getValue(param, v), getBlock(param, knownBlock));
                }
            }
            return phi.asLocal();
        }

        public Value visit(final MethodContext param, final InstanceInvocationValue node) {
            MethodElement target = node.getInvocationTarget();
            Type returnType = target.getReturnType();
            Call call = getBlock(param, node).call(typeOf(returnType), getFunctionOf(param, target.getEnclosingType().validate().resolve(), target, node.getKind()));
            param.values.put(node, call.asLocal());
            cc.quarkus.qcc.graph.Value instance = node.getInstance();
            call.arg(typeOf(instance.getType()), getValue(param, instance));
            int cnt = node.getArgumentCount();
            for (int i = 0; i < cnt; i++) {
                cc.quarkus.qcc.graph.Value arg = node.getArgument(i);
                call.arg(typeOf(arg.getType()), getValue(param, arg));
            }
            return call.asLocal();
        }

        public Value visit(final MethodContext param, final StaticInvocationValue node) {
            MethodElement target = node.getInvocationTarget();
            Type returnType = target.getReturnType();
            Call call = getBlock(param, node).call(typeOf(returnType), getFunctionOf(param, target.getEnclosingType().validate().resolve(), target, DispatchInvocation.Kind.EXACT));
            param.values.put(node, call.asLocal());
            int cnt = node.getArgumentCount();
            for (int i = 0; i < cnt; i++) {
                cc.quarkus.qcc.graph.Value arg = node.getArgument(i);
                call.arg(typeOf(arg.getType()), getValue(param, arg));
            }
            return call.asLocal();
        }

        public Value visit(final MethodContext param, final Neg node) {
            Type javaInputType = node.getInput().getType();
            Value inputType = typeOf(javaInputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val;
            val = isFloating(javaInputType) ?
                   target.fneg(inputType, llvmInput).asLocal() :
                   target.sub(inputType, Values.ZERO, llvmInput).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Shr node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isSigned(node.getType()) ?
                          target.ashr(inputType, llvmLeft, llvmRight).asLocal() :
                          target.lshr(inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Shl node) {
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            Value val = getBlock(param, node).shl(typeOf(node.getType()), llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Sub node) {
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            Value val = getBlock(param, node).sub(typeOf(node.getType()), llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Div node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.fdiv(inputType, llvmLeft, llvmRight).asLocal() :
                        isSigned(node.getType()) ?
                          target.sdiv(inputType, llvmLeft, llvmRight).asLocal() :
                          target.udiv(inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Mod node) {
            Value inputType = typeOf(node.getType());
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(node.getType()) ?
                          target.frem(inputType, llvmLeft, llvmRight).asLocal() :
                        isSigned(node.getType()) ?
                          target.srem(inputType, llvmLeft, llvmRight).asLocal() :
                          target.urem(inputType, llvmLeft, llvmRight).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final BitCast node) {
            Type javaInputType = node.getInput().getType();
            Type javaOutputType = node.getType();
            Value inputType = typeOf(javaInputType);
            Value outputType = typeOf(javaOutputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = target.bitcast(inputType, llvmInput, outputType).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Convert node) {
            Type javaInputType = node.getInput().getType();
            Type javaOutputType = node.getType();
            Value inputType = typeOf(javaInputType);
            Value outputType = typeOf(javaOutputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(javaInputType) ?
                        isSigned(javaOutputType) ?
                        target.fptosi(inputType, llvmInput, outputType).asLocal() :
                        target.fptoui(inputType, llvmInput, outputType).asLocal() :
                        isSigned(javaInputType) ?
                        target.sitofp(inputType, llvmInput, outputType).asLocal() :
                        target.uitofp(inputType, llvmInput, outputType).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Extend node) {
            Type javaInputType = node.getInput().getType();
            Type javaOutputType = node.getType();
            Value inputType = typeOf(javaInputType);
            Value outputType = typeOf(javaOutputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(javaInputType) ?
                        target.fpext(inputType, llvmInput, outputType).asLocal() :
                        isSigned(javaInputType) ?
                        target.sext(inputType, llvmInput, outputType).asLocal() :
                        target.zext(inputType, llvmInput, outputType).asLocal();
            param.values.put(node, val);
            return val;
        }

        public Value visit(final MethodContext param, final Truncate node) {
            Type javaInputType = node.getInput().getType();
            Type javaOutputType = node.getType();
            Value inputType = typeOf(javaInputType);
            Value outputType = typeOf(javaOutputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val = isFloating(javaInputType) ?
                        target.ftrunc(inputType, llvmInput, outputType).asLocal() :
                        target.trunc(inputType, llvmInput, outputType).asLocal();
            param.values.put(node, val);
            return val;
        }
    };

    private final TerminatorVisitor<MethodContext, Void> terminatorVisitor = new TerminatorVisitor<MethodContext, Void>() {
        public Void visit(final MethodContext param, final If node) {
            BasicBlock tb = node.getTrueBranch();
            BasicBlock fb = node.getFalseBranch();
            getBlock(param, node).br(getValue(param, node.getCondition()), getBlock(param, tb), getBlock(param, fb));
            return null;
        }

        public Void visit(final MethodContext param, final Goto node) {
            getBlock(param, node).br(getBlock(param, node.getResumeTarget()));
            return null;
        }

        public Void visit(final MethodContext param, final Return node) {
            getBlock(param, node).ret();
            return null;
        }

        public Void visit(final MethodContext param, final ValueReturn node) {
            cc.quarkus.qcc.graph.Value rv = node.getReturnValue();
            getBlock(param, node).ret(typeOf(rv.getType()), getValue(param, rv));
            return null;
        }
    };

    private Value getValue(final MethodContext cache, final cc.quarkus.qcc.graph.Value input) {
        if (input instanceof IntegerLiteral) {
            return Values.intConstant(((IntegerLiteral) input).longValue());
        } else if (input instanceof Literal) {
            throw Assert.unsupported();
        } else {
            Value value = cache.values.get(input);
            if (value == null) {
                value = input.accept(valueVisitor, cache);
                cache.values.put(input, value);
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

    private Value getFunctionOf(final MethodContext cache, final ResolvedTypeDefinition owner, final ParameterizedExecutableElement invocationTarget, final DispatchInvocation.Kind kind) {
        if (invocationTarget instanceof MethodElement) {
            MethodElement methodElement = (MethodElement) invocationTarget;
            compileMethod(methodElement, kind != DispatchInvocation.Kind.EXACT);
        } else {
            assert invocationTarget instanceof ConstructorElement;
            ConstructorElement constructorElement = (ConstructorElement) invocationTarget;
            compileMethod(constructorElement, kind != DispatchInvocation.Kind.EXACT);
        }
        return getMethod(owner, invocationTarget).asGlobal();

    }

    private cc.quarkus.qcc.machine.llvm.BasicBlock getBlock(final MethodContext cache, final Node node) {
        return getBlock(cache, cache.schedule.getBlockForNode(node));
    }

    private cc.quarkus.qcc.machine.llvm.BasicBlock getBlock(final MethodContext cache, final BasicBlock bb) {
        cc.quarkus.qcc.machine.llvm.BasicBlock target = cache.blocks.get(bb);
        if (target == null) {
            target = cache.def.createBlock();
            cache.blocks.put(bb, target);
        }
        return target;
    }

    static final class MethodContext {
        final ParameterizedExecutableElement currentMethod;
        final FunctionDefinition def;
        final Map<cc.quarkus.qcc.graph.Value, cc.quarkus.qcc.machine.llvm.Value> values = new HashMap<>();
        final Map<cc.quarkus.qcc.graph.BasicBlock, cc.quarkus.qcc.machine.llvm.BasicBlock> blocks = new HashMap<>();
        final Set<cc.quarkus.qcc.graph.BasicBlock> processed = new HashSet<>();
        final Set<cc.quarkus.qcc.graph.BasicBlock> knownBlocks;
        final Schedule schedule;
        final Set<Node> built = new HashSet<>();

        MethodContext(final ParameterizedExecutableElement currentMethod, final FunctionDefinition def, final Set<BasicBlock> knownBlocks, final Schedule schedule) {
            this.currentMethod = currentMethod;
            this.def = def;
            this.knownBlocks = knownBlocks;
            this.schedule = schedule;
        }
    }
}
