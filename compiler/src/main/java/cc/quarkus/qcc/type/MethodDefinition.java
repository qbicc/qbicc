package cc.quarkus.qcc.type;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.interpret.Heap;
import cc.quarkus.qcc.interpret.SimpleHeap;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface MethodDefinition extends MethodDescriptor {

    InsnList getInstructions();

    List<TryCatchBlockNode> getTryCatchBlocks();

    int getMaxLocals();

    int getMaxStack();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();

    default CallResult call(Object... arguments) {
        return call(new SimpleHeap(), arguments);
    }

    default CallResult call(List<Object> arguments) {
        return call(new SimpleHeap(), arguments);
    }

    CallResult call(Heap heap, Object... arguments);

    CallResult call(Heap heap, List<Object> arguments);

    default void writeGraph(String path) throws IOException {
        writeGraph(Paths.get(path));
    }

    void writeGraph(Path path) throws IOException;
}
