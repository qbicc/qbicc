package cc.quarkus.qcc.type.definition;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.GraphBuilder;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class MethodDefinitionNode<V> extends MethodNode implements MethodDefinition<V> {

    public MethodDefinitionNode(TypeDefinitionNode typeDefinition, int access, String name, MethodDescriptor methodDescriptor, String signature, String[] exceptions) {
        super(Universe.ASM_VERSION, access, name, methodDescriptor.getDescriptor(), signature, exceptions);
        this.typeDefinition = typeDefinition;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public MethodGraph getGraph() {
        return this.graph.updateAndGet((prev) -> {
            if (prev != null) {
                return prev;
            }
            GraphBuilder builder = new GraphBuilder(this.access, this.name, this.desc, this.signature, this.exceptions.toArray(new String[0]));
            accept(builder);
            return new MethodGraphImpl(builder.getParameters(), builder.getEntryBlock());
        });
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
    public List<Type> getParamTypes() {
        return this.methodDescriptor.getParamTypes();
    }

    @Override
    public Type getReturnType() {
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
        return (getMethodNode().access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public boolean isSynchronized() {
        return (getMethodNode().access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    @Override
    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    @Override
    public String toString() {
        return this.typeDefinition + " " + this.name + this.desc;
    }

    private final MethodDescriptor methodDescriptor;

    private final TypeDefinitionNode typeDefinition;

    private AtomicReference<MethodGraph> graph = new AtomicReference<>();

}
