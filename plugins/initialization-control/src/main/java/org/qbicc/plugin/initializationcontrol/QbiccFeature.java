package org.qbicc.plugin.initializationcontrol;

final class QbiccFeature {
    String[] initializeAtRuntime;
    String[] runtimeResource;  // ClassLoader.findResource
    String[] runtimeResources; // ClassLoader.findResources
}
