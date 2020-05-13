package cc.quarkus.qcc.type.universe;

import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;

public class Core {

    public static class java {
        public static class lang {
            static final AtomicReference<TypeDefinition> _Object = new AtomicReference<>();

            static final AtomicReference<TypeDefinition> _String = new AtomicReference<>();

            public static TypeDefinition Object() {
                return Universe.instance().findClass("java/lang/Object");
            }

            public static TypeDefinition Throwable() {
                return Universe.instance().findClass("java/lang/Throwable");
            }

            public static TypeDefinition String() {
                return Universe.instance().findClass("java/lang/String");
            }
        }
    }
}
