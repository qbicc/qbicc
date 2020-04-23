package cc.quarkus.qcc.type;

import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface MethodDefinition extends MethodDescriptor {

    InsnList getInstructions();
    List<TryCatchBlockNode> getTryCatchBlocks();

    int getMaxLocals();
    int getMaxStack();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();
}
