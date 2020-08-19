package cc.quarkus.qcc.compiler.native_image.api;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A native image generator implementation.  A program is built by tracing the execution path of each entry point method.
 */
public interface NativeImageGenerator {
    /**
     * Add an entry point to this native image.  The entry point must have {@code C} linkage.
     *
     * @param methodDefinition the entry point
     */
    void addEntryPoint(MethodElement methodDefinition);

    /**
     * Compile the program into an image.
     */
    void compile();
}
