package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface MethodDefinition extends MethodIdentifier {

    InsnList getInstructions();

    ResolvedMethodBody getGraph();

    List<TryCatchBlockNode> getTryCatchBlocks();

    int getMaxLocals();

    int getMaxStack();

    boolean isPublic();
    boolean isPrivate();
    boolean isSynchronized();
    boolean isVarargs();
    boolean isNative();

    TypeDefinition getTypeDefinition();



}
