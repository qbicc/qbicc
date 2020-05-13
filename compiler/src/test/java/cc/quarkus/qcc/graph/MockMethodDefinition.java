package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.interpret.InterpreterThread;
import cc.quarkus.qcc.type.CallResult;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class MockMethodDefinition<V> implements MethodDefinition<V> {
    @Override
    public InsnList getInstructions() {
        return null;
    }

    @Override
    public List<TryCatchBlockNode> getTryCatchBlocks() {
        return null;
    }

    @Override
    public int getMaxLocals() {
        return 0;
    }

    @Override
    public int getMaxStack() {
        return 0;
    }

    @Override
    public boolean isSynchronized() {
        return false;
    }

    @Override
    public TypeDefinition getTypeDefinition() {
        return null;
    }

    @Override
    public CallResult<V> call(InterpreterThread thread, Object... arguments) {
        return null;
    }

    @Override
    public CallResult<V> call(InterpreterThread thread, List<Object> arguments) {
        return null;
    }

    @Override
    public void writeGraph(Path path) throws IOException {

    }

    @Override
    public String getDescriptor() {
        return null;
    }

    @Override
    public TypeDefinition getOwner() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public List<TypeDescriptor<?>> getParamTypes() {
        return Collections.emptyList();
    }

    @Override
    public TypeDescriptor<V> getReturnType() {
        return null;
    }
}
