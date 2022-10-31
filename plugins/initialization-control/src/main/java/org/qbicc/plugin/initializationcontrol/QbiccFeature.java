package org.qbicc.plugin.initializationcontrol;

final class QbiccFeature {
    String[] initializeAtRuntime;
    String[] runtimeResource;  // ClassLoader.findResource
    String[] runtimeResources; // ClassLoader.findResources
    Method[] reflectiveMethods;
    Field[] reflectiveFields;

    static final class Method {
        String declaringClass;
        String name;
        String descriptor;
    }

    static final class Field {
        String declaringClass;
        String name;
    }
}
