package cc.quarkus.qcc.type.definition;

import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.interpret.SimpleInterpreterHeap;
import cc.quarkus.qcc.interpret.InterpreterThread;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public interface TypeDefinition {
    int getAccess();

    String getName();

    TypeDefinition getSuperclass();

    List<TypeDefinition> getInterfaces();

    Set<MethodDefinition<?>> getMethods();

    MethodDefinition<?> findMethod(String name, String desc);

    <V extends QType> MethodDefinition<V> findMethod(MethodDescriptor<V> methodDescriptor);

    <V extends QType> FieldDefinition<V> findField(String name);

    boolean isAssignableFrom(TypeDefinition other);

    TypeDescriptor<ObjectReference> getTypeDescriptor();

    <V extends QType> V getStatic(FieldDefinition<V> field);

    <V extends QType> V getField(FieldDefinition<V> field, ObjectReference objRef);

    <V extends QType> void putField(FieldDefinition<V> field, ObjectReference objRef, V val);

    default ObjectReference newInstance(Object... arguments) {
        return newInstance(new InterpreterThread(new SimpleInterpreterHeap()), arguments);
    }

    default ObjectReference newInstance(List<Object> arguments) {
        return newInstance(new InterpreterThread(new SimpleInterpreterHeap()), arguments);
    }

    ObjectReference newInstance(InterpreterThread thread, Object... arguments);

    ObjectReference newInstance(InterpreterThread thread, List<Object> arguments);
}
