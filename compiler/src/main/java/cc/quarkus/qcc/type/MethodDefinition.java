package cc.quarkus.qcc.type;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import cc.quarkus.qcc.graph.type.EndToken;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface MethodDefinition extends MethodDescriptor {

    InsnList getInstructions();
    List<TryCatchBlockNode> getTryCatchBlocks();

    int getMaxLocals();
    int getMaxStack();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();

    CallResult call(Object...arguments);
    CallResult call(List<Object> arguments);

    default void writeGraph(String path) throws IOException {
        writeGraph(Paths.get(path));
    }

    void writeGraph(Path path) throws IOException;
}
