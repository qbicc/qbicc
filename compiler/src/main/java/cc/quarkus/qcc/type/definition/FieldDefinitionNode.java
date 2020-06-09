package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.TypeDescriptorParser;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.tree.FieldNode;

public class FieldDefinitionNode<V> extends FieldNode implements FieldDefinition<V> {

    public FieldDefinitionNode(TypeDefinition typeDefinition,
                               final int access,
                               final String name,
                               final String descriptor,
                               final String signature,
                               final Object value) {
        super(Universe.ASM_VERSION, access, name, descriptor, signature, value);
        this.typeDefinition = typeDefinition;
    }

    @Override
    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    @Override
    public Type getTypeDescriptor() {
        Type type = this.type;
        if (type == null) {
            synchronized (this) {
                type = this.type;
                if (type == null) {
                    TypeDescriptorParser parser = new TypeDescriptorParser(Universe.instance(), desc);
                    this.type = type = parser.parseType();
                }
            }
        }
        return type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public V get(ObjectReference objRef) {
        return this.typeDefinition.getField(this, objRef);
    }

    @Override
    public void put(ObjectReference objRef, V val) {
        this.typeDefinition.putField(this, objRef, val);
    }

    @Override
    public String toString() {
        return this.typeDefinition.getName() + "::" + getName();
    }

    private final TypeDefinition typeDefinition;
    private volatile Type type;
}
