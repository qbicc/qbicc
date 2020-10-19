package cc.quarkus.qcc.type.definition;

import java.util.function.Consumer;

import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public interface ValidatedTypeDefinition extends DefinedTypeDefinition {
    default ValidatedTypeDefinition validate() {
        return this;
    }

    TypeIdLiteral getTypeId();

    ValidatedTypeDefinition getSuperClass();

    ValidatedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    default boolean isSubtypeOf(ValidatedTypeDefinition other) {
        if (other.getTypeId() == this.getTypeId()) {
            return true;
        }
        if (hasSuperClass() && getSuperClass().isSubtypeOf(other)) {
            return true;
        }
        int cnt = getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            if (getInterface(i).isSubtypeOf(other)) {
                return true;
            }
        }
        return false;
    }

    FieldSet getInstanceFieldSet();

    FieldSet getStaticFieldSet();

    FieldElement getField(int index);

    default void eachField(Consumer<FieldElement> consumer) {
        int cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            consumer.accept(getField(i));
        }
    }

    MethodElement getMethod(int index);

    ConstructorElement getConstructor(int index);

    InitializerElement getInitializer();


    ResolvedTypeDefinition resolve() throws ResolutionFailedException;
}
