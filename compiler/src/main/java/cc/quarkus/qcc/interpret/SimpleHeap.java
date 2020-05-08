package cc.quarkus.qcc.interpret;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.TypeDefinition;

public class SimpleHeap implements Heap {

    @Override
    public ObjectReference newObject(TypeDefinition type) {
        ObjectReference obj = new ObjectReference(type);
        this.allocated.add(obj);
        return obj;
    }

    @Override
    public Collection<ObjectReference> allocated() {
        return this.allocated;
    }

    private List<ObjectReference> allocated = new ArrayList<>();
}
