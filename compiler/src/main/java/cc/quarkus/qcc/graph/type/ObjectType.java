package cc.quarkus.qcc.graph.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.type.Core;
import cc.quarkus.qcc.type.TypeDefinition;

public class ObjectType implements ConcreteType<Object> {

    public static class java {
        public static class lang {
            public static final ObjectType Object = new ObjectType(Core.java.lang.Object());
            public static final ObjectType String = new ObjectType(Core.java.lang.String());
        }
    }

    private static Map<TypeDefinition, ObjectType> CACHE = new ConcurrentHashMap<>();

    public static ObjectType of(TypeDefinition definition) {
        return CACHE.computeIfAbsent(definition, ObjectType::new);
    }

    protected ObjectType(TypeDefinition definition) {
        this.definition = definition;
    }

    public boolean isAssignableFrom(Type type) {
        return true;
    }

    @Override
    public String label() {
        return this.definition.getName();
    }

    private final TypeDefinition definition;

}
