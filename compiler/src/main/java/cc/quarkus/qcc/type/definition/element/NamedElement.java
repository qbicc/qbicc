package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public interface NamedElement extends BasicElement {
    boolean hasName();

    String getName();

    boolean nameEquals(String name);

    interface Builder extends BasicElement.Builder {
        void setName(String name);

        NamedElement build();
    }
}
