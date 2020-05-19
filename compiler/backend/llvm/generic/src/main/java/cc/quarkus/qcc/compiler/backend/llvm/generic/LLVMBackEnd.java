package cc.quarkus.qcc.compiler.backend.llvm.generic;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cc.quarkus.qcc.compiler.backend.api.BackEnd;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.AddNode;
import cc.quarkus.qcc.graph.node.BinaryIfNode;
import cc.quarkus.qcc.graph.node.BinaryNode;
import cc.quarkus.qcc.graph.node.CompareOp;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.IOProjection;
import cc.quarkus.qcc.graph.node.IfFalseProjection;
import cc.quarkus.qcc.graph.node.IfNode;
import cc.quarkus.qcc.graph.node.IfTrueProjection;
import cc.quarkus.qcc.graph.node.MemoryProjection;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.ParameterProjection;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.node.UnaryIfNode;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.llvm.BasicBlock;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
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
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.definition.MethodDefinitionNode;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import io.smallrye.common.constraint.Assert;
import org.objectweb.asm.tree.AnnotationNode;

public final class LLVMBackEnd implements BackEnd {
    public LLVMBackEnd() {}

    public String getName() {
        return "llvm-generic";
    }

    public void compile(final Universe universe) {
        // fail fast if there's no context
        final Context context = Context.requireCurrent();
        // todo: get config from context
        LLVMBackEndConfig config = new LLVMBackEndConfig() {
            public Optional<List<String>> entryPointClassNames() {
                return Optional.of(List.of("hello.world.Main"));
            }
        };

        // find llc
        // TODO: get target platform from config
        final LlcTool llc = ToolProvider.findAllTools(LlcToolImpl.class, Platform.HOST_PLATFORM, t -> true, LLVMBackEnd.class.getClassLoader()).iterator().next();
        // find C compiler
        final CCompiler cc = ToolProvider.findAllTools(CCompiler.class, Platform.HOST_PLATFORM, t -> true, LLVMBackEnd.class.getClassLoader()).iterator().next();
        final Optional<List<String>> names = config.entryPointClassNames();
        final ArrayDeque<TypeDefinition> classQueue = new ArrayDeque<>();
        for (String className : names.orElse(List.of())) {
            classQueue.addLast(universe.findClass(className));
        }
        final ArrayDeque<MethodDefinitionNode<?>> methodQueue = new ArrayDeque<>();
        while (! classQueue.isEmpty()) {
            final TypeDefinition def = classQueue.removeFirst();
            for (MethodDefinition<?> method : def.getMethods()) {
                // hate this
                MethodDefinitionNode<?> node = (MethodDefinitionNode<?>) method;
                final List<AnnotationNode> visibleAnnotations = node.visibleAnnotations;
                // ASM is terrible
                if (visibleAnnotations != null) {
                    for (AnnotationNode visibleAnnotation : visibleAnnotations) {
                        if (visibleAnnotation.desc.equals("Lcc/quarkus/c_native/api/CNative$extern;")) {
                            // emit method, but this isn't really the way we're going to do it at all
                            methodQueue.addLast(node);
                        }
                    }
                }
            }
        }
        final Module module = LLVM.newModule();
        // now just emit the methods raw
        while (! methodQueue.isEmpty()) {
            final MethodDefinitionNode<?> node = methodQueue.removeFirst();
            final FunctionDefinition func = module.define(node.name).callingConvention(CallingConvention.C).linkage(Linkage.EXTERNAL).returns(typeOf(node.getReturnType()));
            int idx = 0;
            final List<TypeDescriptor<?>> paramTypes = node.getParamTypes();
            final Cache cache = new Cache();
            final List<Value> paramVals = cache.paramValues;
            for (TypeDescriptor<?> paramType : paramTypes) {
                paramVals.add(func.param(typeOf(paramType)).name("p" + idx++).asValue());
            }
            final Graph<?> graph = node.getGraph();
            try {
                node.writeGraph("/tmp/graph.dot");
            } catch (IOException e) {
                e.printStackTrace();
            }
            final StartNode start = graph.getStart();
            processNode(cache, func, start);
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

    private Value typeOf(final TypeDescriptor<?> type) {
        switch (type.label()) {
            case "int8": return i8;
            case "int16":
            case "char": return i16;
            case "int32": return i32;
            case "int64": return i64;
            case "cc/quarkus/c_native/api/CNative$c_int": return i32; // actually we'd probe this
            case "cc/quarkus/c_native/api/CNative$ptr": return ptrTo(i8); // don't even get me started
            default: throw new IllegalStateException(type.label());
        }
    }

    private Value valueOf(final Cache cache, final FunctionDefinition func, final BasicBlock block, final AbstractNode<?> node) {
        Value value = cache.nodeValues.get(node);
        if (value != null) {
            return value;
        }
        if (node instanceof ConstantNode) {
            final ConstantNode<?> constantNode = (ConstantNode<?>) node;
            final QType qValue = constantNode.getValue(null);
            if (qValue instanceof QInt32) {
                value = Values.intConstant(((QInt32) qValue).asInt32().value().intValue());
                cache.nodeValues.put(node, value);
                return value;
            }
        } else if (node instanceof ParameterProjection) {
            final ParameterProjection<?> parameterProjection = (ParameterProjection<?>) node;
            return cache.paramValues.get(parameterProjection.index);
        } else if (node instanceof PhiNode) {
            final PhiNode<?> phiNode = (PhiNode<?>) node;
            final Phi phi = block.phi(typeOf(phiNode.inputs.get(0).getTypeDescriptor()));
            for (Node<?> input : phiNode.inputs) {
                final Value inputValue = valueOf(cache, func, block, (AbstractNode<?>) input);
                BasicBlock inputBlock = processRegion(cache, func, (RegionNode) input.getPredecessors().get(0));
                phi.item(inputValue, inputBlock);
            }
            return phi.asLocal();
        }
        throw Assert.unsupported();
    }

    private void processNode(final Cache cache, final FunctionDefinition func, final StartNode node) {
        cache.basicBlocksByRegion.put(node, func);
        processSuccessors(cache, func, func, node.getSuccessors());
    }

    private void processNode(final Cache cache, final FunctionDefinition func, final BasicBlock block, final Node<?> node) {
        if (cache.visited.putIfAbsent(node, block) != null) {
            return;
        }
        if (node instanceof ReturnNode) {
            processNode(cache, func, block, (ReturnNode<?>) node);
        } else if (node instanceof IfNode) {
            processNode(cache, func, block, (IfNode) node);
        } else if (node instanceof PhiNode) {
            processNode(cache, func, block, (PhiNode<?>) node);
        } else if (node instanceof RegionNode) {
            final BasicBlock dest = processRegion(cache, func, (RegionNode) node);
            if (dest != null) {
                block.br(dest);
            }
        } else if (node instanceof IOProjection || node instanceof MemoryProjection || node instanceof ConstantNode || node instanceof ParameterProjection) {
            processSuccessors(cache, func, block, node.getSuccessors());
        } else if (node instanceof EndNode) {
            // no operation
        } else if (node instanceof BinaryNode) {
            final BinaryNode<?, ?> binaryNode = (BinaryNode<?, ?>) node;
            if (! cache.nodeValues.containsKey(binaryNode)) {
                final AbstractNode<?> lhs = (AbstractNode<?>) binaryNode.getLHS();
                final AbstractNode<?> rhs = (AbstractNode<?>) binaryNode.getRHS();

                final Value value;
                if (node instanceof AddNode) {
                    value = block.add(typeOf(lhs.getTypeDescriptor()), valueOf(cache, func, block, lhs), valueOf(cache, func, block, rhs)).asLocal();
                } else {
                    throw Assert.unsupported();
                }
                cache.nodeValues.put(binaryNode, value);
            }
        } else {
            throw Assert.unsupported();
        }
    }

    private void processSuccessors(final Cache cache, final FunctionDefinition func, final BasicBlock block, final List<? extends Node<?>> successors) {
        for (Node<?> successor : successors) {
            processNode(cache, func, block, successor);
        }
    }

    private BasicBlock processRegion(final Cache cache, final FunctionDefinition func, final RegionNode node) {
        if (node.getSuccessors().get(0) instanceof EndNode) {
            // ignore this fake region!
            return null;
        }
        final Map<ControlNode<?>, BasicBlock> bbr = cache.basicBlocksByRegion;
        final BasicBlock existing = bbr.get(node);
        if (existing != null) {
            // already processed the region contents
            return existing;
        }
        final BasicBlock newBlock = func.createBlock().name("Region" + node.getId());
        bbr.put(node, newBlock);
        for (Node<?> successor : node.getSuccessors()) {
            processNode(cache, func, newBlock, successor);
        }
        return newBlock;
    }

    private void processNode(final Cache cache, final FunctionDefinition func, final BasicBlock block, final PhiNode<?> node) {
        // a phi! process all inputs recursively
        final List<Node<?>> inputs = node.inputs;

        for (Node<?> input : inputs) {
            processNode(cache, func, block, input);
        }
    }

    private void processNode(final Cache cache, final FunctionDefinition func, final BasicBlock block, final IfNode ifNode) {
        final CompareOp op = ifNode.getOp();
        final IfTrueProjection trueProjection = ifNode.getTrueOut();
        final IfFalseProjection falseProjection = ifNode.getFalseOut();
        final RegionNode ifTrueRegion = (RegionNode) trueProjection.getControlSuccessors().get(0);
        final RegionNode ifFalseRegion = (RegionNode) falseProjection.getControlSuccessors().get(0);
        final BasicBlock ifTrue = processRegion(cache, func, ifTrueRegion);
        final BasicBlock ifFalse = processRegion(cache, func, ifFalseRegion);
        final IntCondition cond;
        final Value cmpResult;
        if (ifNode instanceof UnaryIfNode) {
            final UnaryIfNode unaryIfNode = (UnaryIfNode) ifNode;
            switch (op) {
                case NONNULL: cond = IntCondition.ne; break;
                case NULL: cond = IntCondition.eq; break;
                default: throw new IllegalStateException("problem");
            }
            final AbstractNode<?> test = (AbstractNode<?>) unaryIfNode.getTest();
            cmpResult = block.icmp(cond, typeOf(test.getTypeDescriptor()), Values.ZERO, valueOf(cache, func, block, test)).asLocal();
        } else {
            assert ifNode instanceof BinaryIfNode;
            final BinaryIfNode<?> binaryIfNode = (BinaryIfNode<?>) ifNode;
            switch (op) {
                case EQUAL: cond = IntCondition.eq; break;
                case NOT_EQUAL: cond = IntCondition.ne; break;
                case LESS_THAN: cond = IntCondition.slt; break;
                case LESS_THAN_OR_EQUAL: cond = IntCondition.sle; break;
                case GREATER_THAN: cond = IntCondition.sgt; break;
                case GREATER_THAN_OR_EQUAL: cond = IntCondition.sge; break;
                default: throw new IllegalStateException("problem");
            }
            final AbstractNode<?> lhs = (AbstractNode<?>) binaryIfNode.getLHS();
            final AbstractNode<?> rhs = (AbstractNode<?>) binaryIfNode.getRHS();
            cmpResult = block.icmp(cond, typeOf(lhs.getTypeDescriptor()), valueOf(cache, func, block, lhs), valueOf(cache, func, block, rhs)).asLocal();
        }
        block.br(cmpResult, ifTrue, ifFalse);
        // TODO: add phi inputs
    }

    private void processNode(final Cache cache, final FunctionDefinition func, final BasicBlock block, final ReturnNode<?> node) {
        final AbstractNode<?> input = (AbstractNode<?>) node.getInput();
        block.ret(typeOf(input.getTypeDescriptor()), valueOf(cache, func, block, input));
    }

    static final class Cache {
        final List<Value> paramValues = new ArrayList<>();
        final Map<Node<?>, BasicBlock> visited = new IdentityHashMap<>();
        final Map<Node<?>, Value> nodeValues = new IdentityHashMap<>();
        final Map<ControlNode<?>, BasicBlock> basicBlocksByRegion = new IdentityHashMap<>();
    }
}
