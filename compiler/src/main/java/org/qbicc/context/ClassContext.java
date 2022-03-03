package org.qbicc.context;

import java.nio.ByteBuffer;
import java.util.List;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 * A class and interface context, which can either be standalone (static) or can be integrated with an interpreter.  An
 * interpreter should have one instance per class loader.
 */
public interface ClassContext extends DescriptorTypeResolver {
    CompilationContext getCompilationContext();

    /**
     * Get the class loader object for this context.  The bootstrap class loader is {@code null}.
     *
     * @return the class loader object for this context
     */
    VmClassLoader getClassLoader();

    DefinedTypeDefinition findDefinedType(String typeName);

    DefinedTypeDefinition.Builder newTypeBuilder();

    String deduplicate(ByteBuffer buffer, int offset, int length);

    String deduplicate(String original);

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    BasicBlockBuilder newBasicBlockBuilder(ExecutableElement element);

    void defineClass(String name, DefinedTypeDefinition definition);

    ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature);

    /**
     * Get the entirety of the resource with the given name, or {@code null} if no such resource could be found.
     *
     * @param resourceName the resource name (must not be {@code null})
     * @return the resource bytes, or {@code null} if the resource was not found
     */
    byte[] getResource(String resourceName);

    /**
     * Get the entirety of all of the resources with the given name within this class context.
     *
     * @param resourceName the resource name (must not be {@code null})
     * @return the resource list (not {@code null})
     */
    List<byte[]> getResources(String resourceName);
}
