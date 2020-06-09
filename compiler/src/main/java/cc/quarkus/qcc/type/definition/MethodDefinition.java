package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface MethodDefinition<V> extends MethodDescriptor {

    InsnList getInstructions();

    MethodGraph getGraph();

    List<TryCatchBlockNode> getTryCatchBlocks();

    int getMaxLocals();

    int getMaxStack();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();

}
