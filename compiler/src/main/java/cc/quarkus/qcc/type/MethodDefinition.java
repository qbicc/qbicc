package cc.quarkus.qcc.type;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import org.objectweb.asm.tree.InsnList;

public interface MethodDefinition {

    InsnList getInstructions();

    int getMaxLocals();

    int getMaxStack();

    List<ConcreteType<?>> getParamTypes();

    ConcreteType<?> getReturnType();

    boolean isStatic();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();
}
