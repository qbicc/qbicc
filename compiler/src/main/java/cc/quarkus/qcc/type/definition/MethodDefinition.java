package cc.quarkus.qcc.type.definition;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import cc.quarkus.qcc.interpret.InterpreterThread;
import cc.quarkus.qcc.interpret.SimpleInterpreterHeap;
import cc.quarkus.qcc.interpret.CallResult;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface MethodDefinition<V extends QType> extends MethodDescriptor<V> {

    InsnList getInstructions();

    List<TryCatchBlockNode> getTryCatchBlocks();

    int getMaxLocals();

    int getMaxStack();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();

    default CallResult<V> call(Object... arguments) {
        return call(new InterpreterThread(new SimpleInterpreterHeap()), arguments);
    }

    default CallResult<V> call(List<Object> arguments) {
        return call(new InterpreterThread(new SimpleInterpreterHeap()), arguments);
    }

    CallResult<V> call(InterpreterThread thread, Object... arguments);

    CallResult<V> call(InterpreterThread thread, List<Object> arguments);

    default void writeGraph(String path) throws IOException {
        writeGraph(Paths.get(path));
    }

    void writeGraph(Path path) throws IOException;
}
