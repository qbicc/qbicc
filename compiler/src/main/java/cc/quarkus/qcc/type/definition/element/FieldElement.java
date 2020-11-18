package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 *
 */
public interface FieldElement extends AnnotatedElement, NamedElement {
    FieldElement[] NO_FIELDS = new FieldElement[0];

    ValueType getType() throws ResolutionFailedException;

    boolean hasClass2Type();

    default boolean isVolatile() {
        return hasAllModifiersOf(ClassFile.ACC_VOLATILE);
    }

    interface TypeResolver {
        ValueType resolveFieldType(long argument) throws ResolutionFailedException;

        boolean hasClass2FieldType(long argument);

        // todo: generic/annotated type
    }

    static Builder builder() {
        return new FieldElementImpl.Builder();
    }

    interface Builder extends AnnotatedElement.Builder, NamedElement.Builder {
        void setTypeResolver(TypeResolver resolver, long argument);

        FieldElement build();
    }
}
