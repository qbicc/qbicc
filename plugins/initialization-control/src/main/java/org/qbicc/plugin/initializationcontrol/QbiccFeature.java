package org.qbicc.plugin.initializationcontrol;

public final class QbiccFeature {
    public String[] initializeAtRuntime;
    public String[] runtimeResource;  // ClassLoader.findResource
    public String[] runtimeResources; // ClassLoader.findResources
    public ReflectiveClass[] reflectiveClasses;
    public Constructor[] reflectiveConstructors;
    public Field[] reflectiveFields;
    public Method[] reflectiveMethods;

    public static final class ReflectiveClass {
        String name;
        boolean fields;
        boolean constructors;
        boolean methods;

        public ReflectiveClass() {}
        public ReflectiveClass(String name, boolean fields, boolean constructors, boolean methods) {
            this.name = name;
            this.fields = fields;
            this.constructors = constructors;
            this.methods = methods;
        }
    }

    public static final class Constructor {
        String declaringClass;
        String[] arguments;

        public Constructor() {}
        public Constructor(String declaringClass, String[] arguments) {
            this.declaringClass = declaringClass;
            this.arguments = arguments;
        }
    }

    public static final class Field {
        String declaringClass;
        String name;

        public Field() {}
        public Field(String declaringClass, String name) {
            this.declaringClass = declaringClass;
            this.name = name;
        }
    }

    public static final class Method {
        String declaringClass;
        String name;
        String[] arguments;

        public Method() {} // for YAML deserialization
        public Method(String declaringClass, String name, String[] arguments) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.arguments = arguments;
        }
    }
}
