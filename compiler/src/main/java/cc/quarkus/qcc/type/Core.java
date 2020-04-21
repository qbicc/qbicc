package cc.quarkus.qcc.type;

import java.util.concurrent.atomic.AtomicReference;

public class Core {

    public static class java {
        public static class lang {
            static final AtomicReference<TypeDefinition> _Object = new AtomicReference<>();

            static final AtomicReference<TypeDefinition> _String = new AtomicReference<>();

            public static TypeDefinition Object() {
                return Universe.instance().findClass("java/lang/Object");
            }

            public static TypeDefinition String() {
                return Universe.instance().findClass("java/lang/String");
            }
        }
    }
}
