package cc.quarkus.qcc.compiler.backend.llvm.generic;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import cc.quarkus.qcc.compiler.backend.api.BackEnd;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.ParameterProjection;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
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
        // I can only apologise for this
        Method getGraph;
        try {
            getGraph = MethodDefinitionNode.class.getDeclaredMethod("getGraph");
            getGraph.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
        final Module module = LLVM.newModule();
        // now just emit the methods raw
        while (! methodQueue.isEmpty()) {
            final MethodDefinitionNode<?> node = methodQueue.removeFirst();
            final FunctionDefinition func = module.define(node.name).callingConvention(CallingConvention.C).linkage(Linkage.EXTERNAL).returns(typeOf(node.getReturnType()));
            int idx = 0;
            final List<TypeDescriptor<?>> paramTypes = node.getParamTypes();
            final List<Value> paramVals = Arrays.asList(new Value[ paramTypes.size() ]);
            for (TypeDescriptor<?> paramType : paramTypes) {
                final Value pVal = func.param(typeOf(paramType)).name("p" + idx).asValue();
                paramVals.set(idx++, pVal);
            }
            final Graph<?> graph;
            try {
                graph = (Graph<?>) getGraph.invoke(node);
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                throw new UndeclaredThrowableException(e);
            }
            try {
                node.writeGraph("/tmp/graph.dot");
            } catch (IOException e) {
                e.printStackTrace();
            }
            final EndNode<?> end = graph.getEnd();
            processNode(paramVals, func, end);
        }
        try {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(System.out))) {
                module.writeTo(w);
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private Value processValueNode(final List<Value> paramVals, final FunctionDefinition func, final AbstractNode<?> node) {
        if (node instanceof ConstantNode) {
            final ConstantNode<?> constantNode = (ConstantNode<?>) node;
            final QType value = constantNode.getValue(null);
            if (value instanceof QInt32) {
                return Values.intConstant(((QInt32) value).asInt32().value().intValue());
            }
        } else if (node instanceof ParameterProjection) {
            final ParameterProjection<?> parameterProjection = (ParameterProjection<?>) node;
            return paramVals.get(parameterProjection.index);
        }
        throw Assert.unsupported();
    }

    private void processNode(final List<Value> paramVals, final FunctionDefinition func, final EndNode<?> node) {
        final Node<?> completion = node.getCompletion();
        processNode(paramVals, func, completion);
    }

    private void processNode(final List<Value> paramVals, final FunctionDefinition func, final Node<?> node) {
        if (node instanceof ReturnNode) {
            processNode(paramVals, func, (ReturnNode<?>) node);
        } else if (node instanceof ConstantNode) {
            // no action needed
        } else if (node instanceof ParameterProjection) {
            // no action needed
        } else {
            throw Assert.unsupported();
        }
    }

    private void processNode(final List<Value> paramVals, final FunctionDefinition func, final ReturnNode<?> node) {
        final AbstractNode<?> input = (AbstractNode<?>) node.getInput();
        processNode(paramVals, func, input);
        func.ret(typeOf(input.getTypeDescriptor()), processValueNode(paramVals, func, input));
    }
}
