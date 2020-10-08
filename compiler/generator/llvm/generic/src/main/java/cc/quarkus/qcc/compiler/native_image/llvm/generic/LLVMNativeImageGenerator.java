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
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BooleanType;
import cc.quarkus.qcc.graph.CatchValue;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.CommutativeBinaryValue;
import cc.quarkus.qcc.graph.ConstantValue;
import cc.quarkus.qcc.graph.FloatType;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.GraphVisitor;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.IfValue;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.InstanceInvocationValue;
import cc.quarkus.qcc.graph.IntegerType;
import cc.quarkus.qcc.graph.Invocation;
import cc.quarkus.qcc.graph.InvocationValue;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NonCommutativeBinaryValue;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.SignedIntegerType;
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
        llcInv.setMessageHandler(ToolMessageHandler.REPORTING);
        try {
            llcInv.invoke();
        } catch (IOException e) {
            Context.error(null, "LLVM compilation failed: %s", e);
            return;
        }
        // ▪ analyze the written object file
        try (ObjectFile program = objProvider.openObjectFile(programPath)) {

        } catch (IOException e) {
            Context.error(null, "Failed to read object file \"%s\": %s", programPath, e);
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
        ldInv.setMessageHandler(ToolMessageHandler.REPORTING);
        ldInv.addObjectFiles(objects);
        try {
            ldInv.invoke();
        } catch (IOException e) {
            Context.error(null, "Linking failed: %s", e);
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
        final FunctionDefinition func = getMethod(definition.getEnclosingType().verify().resolve(), definition).callingConvention(CallingConvention.C).linkage(Linkage.EXTERNAL);
        int idx = 0;
        if (virtual) {
            throw new UnsupportedOperationException("Virtual dispatch");
        }
        MethodHandle methodHandle = definition.getMethodBody();
        if (methodHandle == null) {
            // nothing to do
            return;
        }
        MethodBody methodBody = methodHandle.getResolvedMethodBody();
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
            block.getTerminator().accept(visitor, cache);
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

    private final GraphVisitor<MethodContext> visitor = new GraphVisitor<MethodContext>() {
        public void visitUnknown(final MethodContext param, final Node node) {
            throw new IllegalStateException("Unsupported node: " + node);
        }

        public void visit(final MethodContext param, final CommutativeBinaryValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            Value outputType = typeOf(node.getType());
            Type javaInputType = node.getLeftInput().getType();
            Value inputType = typeOf(javaInputType);
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val;
            switch (node.getKind()) {
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
            param.values.put(node, val);
        }

        public void visit(final MethodContext param, final ConstantValue node) {
            // already cached
        }

        public void visit(final MethodContext param, final Goto node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            getBlock(param, node).br(getBlock(param, node.getTarget()));
        }

        public void visit(final MethodContext param, final If node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            BasicBlock tb = node.getTrueBranch();
            BasicBlock fb = node.getFalseBranch();
            getBlock(param, node).br(getValue(param, node.getCondition()), getBlock(param, tb), getBlock(param, fb));
        }

        public void visit(final MethodContext param, final IfValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            cc.quarkus.qcc.graph.Value trueValue = node.getTrueValue();
            Value inputType = typeOf(trueValue.getType());
            cc.quarkus.qcc.graph.Value falseValue = node.getFalseValue();
            Value val = getBlock(param, node).select(typeOf(node.getCondition().getType()), getValue(param, node.getCondition()), inputType, getValue(param, trueValue), getValue(param, falseValue)).asLocal();
            param.values.put(node, val);
        }

        public void visit(final MethodContext param, final CatchValue node) {
            // nothing for now; todo: landingpad, exception object decoding
        }

        public void visit(final MethodContext param, final InstanceInvocation node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            ParameterizedExecutableElement target = node.getInvocationTarget();
            Call call = getBlock(param, node).call(void_, getFunctionOf(param, target.getEnclosingType().verify().resolve(), target, node.getKind()));
            cc.quarkus.qcc.graph.Value instance = node.getInstance();
            call.arg(typeOf(instance.getType()), getValue(param, instance));
            int cnt = node.getArgumentCount();
            for (int i = 0; i < cnt; i++) {
                cc.quarkus.qcc.graph.Value arg = node.getArgument(i);
                call.arg(typeOf(arg.getType()), getValue(param, arg));
            }
        }

        public void visit(final MethodContext param, final InstanceInvocationValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            ParameterizedExecutableElement target = node.getInvocationTarget();
            Type returnType = ((MethodElement) target).getReturnType();
            Call call = getBlock(param, node).call(typeOf(returnType), getFunctionOf(param, target.getEnclosingType().verify().resolve(), target, node.getKind()));
            param.values.put(node, call.asLocal());
            cc.quarkus.qcc.graph.Value instance = node.getInstance();
            call.arg(typeOf(instance.getType()), getValue(param, instance));
            int cnt = node.getArgumentCount();
            for (int i = 0; i < cnt; i++) {
                cc.quarkus.qcc.graph.Value arg = node.getArgument(i);
                call.arg(typeOf(arg.getType()), getValue(param, arg));
            }
        }

        public void visit(final MethodContext param, final Invocation node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            ParameterizedExecutableElement target = node.getInvocationTarget();
            Call call = getBlock(param, node).call(void_, getFunctionOf(param, target.getEnclosingType().verify().resolve(), target, InstanceInvocation.Kind.EXACT));
            int cnt = node.getArgumentCount();
            for (int i = 0; i < cnt; i++) {
                cc.quarkus.qcc.graph.Value arg = node.getArgument(i);
                call.arg(typeOf(arg.getType()), getValue(param, arg));
            }
        }

        public void visit(final MethodContext param, final InvocationValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            ParameterizedExecutableElement target = node.getInvocationTarget();
            Type returnType = ((MethodElement) target).getReturnType();
            Call call = getBlock(param, node).call(typeOf(returnType), getFunctionOf(param, target.getEnclosingType().verify().resolve(), target, InstanceInvocation.Kind.EXACT));
            param.values.put(node, call.asLocal());
            int cnt = node.getArgumentCount();
            for (int i = 0; i < cnt; i++) {
                cc.quarkus.qcc.graph.Value arg = node.getArgument(i);
                call.arg(typeOf(arg.getType()), getValue(param, arg));
            }
        }

        public void visit(final MethodContext param, final NonCommutativeBinaryValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            Value outputType = typeOf(node.getType());
            Type javaInputType = node.getLeftInput().getType();
            Value inputType = typeOf(javaInputType);
            Value llvmLeft = getValue(param, node.getLeftInput());
            Value llvmRight = getValue(param, node.getRightInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val;
            switch (node.getKind()) {
                case SHR: val = (javaInputType.isUnsigned() ? target.lshr(inputType, llvmLeft, llvmRight) : target.ashr(inputType, llvmLeft, llvmRight)).asLocal(); break;
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
            param.values.put(node, val);
        }

        public void visit(final MethodContext param, final ParameterValue node) {
            // already cached
        }

        public void visit(final MethodContext param, final PhiValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
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
        }

        public void visit(final MethodContext param, final Return node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            getBlock(param, node).ret();
        }

        public void visit(final MethodContext param, final TryThrow node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            getBlock(param, node).br(getBlock(param, node.getCatchHandler()));
        }

        public void visit(final MethodContext param, final UnaryValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            Type javaInputType = node.getInput().getType();
            Value inputType = typeOf(javaInputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val;
            switch (node.getKind()) {
                case NEGATE: val = isFloating(javaInputType) ?
                                   target.fneg(inputType, llvmInput).asLocal() :
                                   target.sub(inputType, Values.ZERO, llvmInput).asLocal(); break;
                default: throw new IllegalStateException();
            }
            param.values.put(node, val);
        }

        public void visit(final MethodContext param, final ValueReturn node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            cc.quarkus.qcc.graph.Value rv = node.getReturnValue();
            getBlock(param, node).ret(typeOf(rv.getType()), getValue(param, rv));
        }

        public void visit(final MethodContext param, final WordCastValue node) {
            if (! param.built.add(node)) {
                return;
            }
            visitDependencies(param, node);
            Type javaInputType = node.getInput().getType();
            Type javaOutputType = node.getType();
            Value inputType = typeOf(javaInputType);
            Value outputType = typeOf(javaOutputType);
            Value llvmInput = getValue(param, node.getInput());
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(param, node);
            Value val;
            switch (node.getKind()) {
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
            param.values.put(node, val);
        }
    };

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

    private Value getFunctionOf(final MethodContext cache, final ResolvedTypeDefinition owner, final ParameterizedExecutableElement invocationTarget, final InstanceInvocation.Kind kind) {
        if (invocationTarget instanceof MethodElement) {
            MethodElement methodElement = (MethodElement) invocationTarget;
            compileMethod(methodElement, kind != InstanceInvocation.Kind.EXACT);
        } else {
            assert invocationTarget instanceof ConstructorElement;
            ConstructorElement constructorElement = (ConstructorElement) invocationTarget;
            compileMethod(constructorElement, kind != InstanceInvocation.Kind.EXACT);
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
