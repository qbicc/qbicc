package cc.quarkus.qcc.graph.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.type.Core;
import cc.quarkus.qcc.type.TypeDefinition;

public class ObjectType implements ConcreteType<ObjectValue> {

    public static class java {
        public static class lang {
            public static final ObjectType Object = new ObjectType(Core.java.lang.Object());
            public static final ObjectType String = new ObjectType(Core.java.lang.String()) {
                @Override
                public ObjectValue newInstance(Object... args) {
                    checkNewInstanceArguments(args, String.class);
                    return new ObjectValue(String, args[0]);
                }
            };
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
        System.err.println( "can I be assigned from " + type);
        return true;
    }

    @Override
    public ObjectValue newInstance(Object... args) {
        // whut?
        return null;
    }

    @Override
    public String label() {
        return "type: " + this.definition.getName();
    }

    public String toString() {
        return label();
    }

    private final TypeDefinition definition;

}
