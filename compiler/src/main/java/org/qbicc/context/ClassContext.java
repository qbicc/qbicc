package org.qbicc.context;

import java.nio.ByteBuffer;
import java.util.List;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.type.Primitive;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.MethodTypeId;
import org.qbicc.type.definition.TypeId;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
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

    /**
     * Resolve a method type from the given descriptor.
     *
     * @param descriptor the method descriptor (must not be {@code null})
     * @return the resolved method type, or {@code null} if the method type isn't resolvable from this context
     */
    MethodTypeId resolveMethodType(MethodDescriptor descriptor);

    /**
     * Resolve a descriptor to a type ID, loading the corresponding class if needed.
     *
     * @param descriptor the descriptor
     * @return the type ID, or {@code null} if the descriptor isn't resolvable from this context
     */
    default TypeId resolveDescriptor(TypeDescriptor descriptor) {
        if (descriptor instanceof ClassTypeDescriptor ctd) {
            final String className = ctd.getPackageName() + '/' + ctd.getClassName();
            final DefinedTypeDefinition definedType = findDefinedType(className);
            return definedType == null ? null : definedType.typeId();
        } else if (descriptor instanceof BaseTypeDescriptor btd) {
            // the VM is what is tracking the primitive class objects for us
            return getCompilationContext().getVm().getPrimitiveClass(Primitive.getPrimitiveFor(btd)).getTypeDefinition().typeId();
        } else if (descriptor instanceof ArrayTypeDescriptor atd) {
            final TypeDescriptor etd = atd.getElementTypeDescriptor();
            if (etd instanceof BaseTypeDescriptor btd) {
                // todo: remove eventually; treat primitive arrays like any array
                return getCompilationContext().getVm().getPrimitiveClass(Primitive.getPrimitiveFor(btd)).getArrayClass().getTypeDefinition().typeId();
            }
            return resolveDescriptor(etd).getArrayTypeId();
        } else {
            throw new IllegalStateException();
        }
    }

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    BasicBlockBuilder newBasicBlockBuilder(BasicBlockBuilder.FactoryContext fc, ExecutableElement element);

    default BasicBlockBuilder newBasicBlockBuilder(ExecutableElement element) {
        return newBasicBlockBuilder(BasicBlockBuilder.FactoryContext.EMPTY, element);
    }

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

    /**
     * Determine whether this is the bootstrap class context.
     *
     * @return {@code true} if this is the bootstrap class context, or {@code false} otherwise.
     */
    boolean isBootstrap();
}
