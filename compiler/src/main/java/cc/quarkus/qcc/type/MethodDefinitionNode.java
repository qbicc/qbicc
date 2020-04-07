package cc.quarkus.qcc.type;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class MethodDefinitionNode extends MethodNode implements MethodDefinition {
    public MethodDefinitionNode(TypeDefinitionNode typeDefinition, int access, String name, String descriptor, String signature, String[] exceptions) {
        super(Universe.ASM_VERSION, access, name, descriptor, signature, exceptions);
        this.typeDefinition = typeDefinition;

        MethodDescriptorParser parser = new MethodDescriptorParser(typeDefinition.getUniverse(), descriptor, isStatic(), typeDefinition);
        this.methodDescriptor = parser.parseMethodDescriptor();
    }

    public MethodNode getMethodNode() {
        return this;
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
    public List<ConcreteType<?>> getParamTypes() {
        return this.methodDescriptor.getParamTypes();
    }

    @Override
    public ConcreteType<?> getReturnType() {
        return this.methodDescriptor.getReturnType();
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

    public boolean isMatching(ConcreteType<?>[] parameterTypes) {
        List<ConcreteType<?>> params = this.methodDescriptor.getParamTypes();
        if ( parameterTypes.length != params.size() ) {
            return false;
        }

        for ( int i = 0 ; i < parameterTypes.length ; ++i ) {
            if ( ! parameterTypes[i].equals(params.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return this.typeDefinition + " " + this.name + this.desc;
    }

    private final MethodDescriptor methodDescriptor;

    private final TypeDefinitionNode typeDefinition;
}
