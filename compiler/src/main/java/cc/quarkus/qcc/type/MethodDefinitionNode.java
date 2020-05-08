package cc.quarkus.qcc.type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.DotWriter;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.interpret.Heap;
import cc.quarkus.qcc.interpret.Interpreter;
import cc.quarkus.qcc.graph.build.GraphBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class MethodDefinitionNode<V> extends MethodNode implements MethodDefinition<V> {

    //public MethodDefinitionNode(TypeDefinitionNode typeDefinition, int access, String name, String descriptor, String signature, String[] exceptions) {
    public MethodDefinitionNode(TypeDefinitionNode typeDefinition, int access, String name, MethodDescriptor<V> methodDescriptor, String signature, String[] exceptions) {
        super(Universe.ASM_VERSION, access, name, methodDescriptor.getDescriptor(), signature, exceptions);
        this.typeDefinition = typeDefinition;

        //MethodDescriptorParser parser = new MethodDescriptorParser(typeDefinition.getUniverse(), typeDefinition, name, descriptor, isStatic());
        this.methodDescriptor = methodDescriptor;
        //this.signature = signature;

    }

    public MethodNode getMethodNode() {
        return this;
    }

    @Override
    public String getDescriptor() {
        return this.methodDescriptor.getDescriptor();
    }

    @Override
    public List<TryCatchBlockNode> getTryCatchBlocks() {
        return this.tryCatchBlocks;
    }

    @Override
    public InsnList getInstructions() {
        return this.instructions;
    }

    @Override
    public int getMaxLocals() {
        return this.maxLocals;
    }

    @Override
    public int getMaxStack() {
        return this.maxStack;
    }

    @Override
    public List<TypeDescriptor<?>> getParamTypes() {
        return this.methodDescriptor.getParamTypes();
    }

    @Override
    public TypeDescriptor<V> getReturnType() {
        return this.methodDescriptor.getReturnType();
    }

    @Override
    public TypeDefinition getOwner() {
        return this.typeDefinition;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isStatic() {
        return ( getMethodNode().access & Opcodes.ACC_STATIC ) != 0;
    }

    @Override
    public boolean isSynchronized() {
        return ( getMethodNode().access & Opcodes.ACC_SYNCHRONIZED ) != 0;
    }

    @Override
    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    @Override
    public CallResult<V> call(Heap heap, Object... arguments) {
        Interpreter<V> interp = new Interpreter<>(heap, getGraph());
        return interp.execute(arguments);
    }

    @Override
    public CallResult<V> call(Heap heap, List<Object> arguments) {
        Interpreter<V> interp = new Interpreter<>(heap, getGraph());
        return interp.execute(arguments);
    }

    @Override
    public void writeGraph(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            path = path.resolve(defaultGraphName());
            path = path.getParent().resolve( path.getFileName() + ".dot");
        }

        Files.createDirectories(path.getParent());


        try ( DotWriter writer = new DotWriter(path) ) {
            writer.write(getGraph());
        }
    }

    protected String defaultGraphName() {
        return getTypeDefinition().getName() + "-" + getName() + getDescriptor();
    }

    protected Graph<V> getGraph() {
        return this.graph.updateAndGet( (prev)->{
            if ( prev != null ) {
                return prev;
            }
            GraphBuilder<V> parser = new GraphBuilder<>(this);
            return parser.build();
        });
    }

    @Override
    public String toString() {
        return this.typeDefinition + " " + this.name + this.desc;
    }

    private final MethodDescriptor<V> methodDescriptor;

    private final TypeDefinitionNode typeDefinition;

    private final AtomicReference<Graph<V>> graph = new AtomicReference<>();
}
