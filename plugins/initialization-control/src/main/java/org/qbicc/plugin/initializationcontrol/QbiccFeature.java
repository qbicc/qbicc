package org.qbicc.plugin.initializationcontrol;

final class QbiccFeature {
    String[] initializeAtRuntime;
    String[] runtimeResource;  // ClassLoader.findResource
    String[] runtimeResources; // ClassLoader.findResources
    Constructor[] reflectiveConstructors;
    Field[] reflectiveFields;
    Method[] reflectiveMethods;

    static final class Constructor {
        String declaringClass;
        String descriptor;
    }

    static final class Field {
        String declaringClass;
        String name;
    }

    static final class Method {
        String declaringClass;
        String name;
        String descriptor;
    }
}
