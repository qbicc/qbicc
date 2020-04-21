package cc.quarkus.qcc.type;

import java.util.List;

import org.objectweb.asm.tree.InsnList;

public interface MethodDefinition extends MethodDescriptor {

    InsnList getInstructions();

    int getMaxLocals();

    int getMaxStack();

    //List<Class<?>> getParamTypes();

    //Class<?> getReturnType();

    //boolean isStatic();

    boolean isSynchronized();

    TypeDefinition getTypeDefinition();
}
