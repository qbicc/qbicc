package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public interface Element {
    int getModifiers();

    int getIndex();

    boolean hasAllModifiersOf(int mask);

    boolean hasNoModifiersOf(int mask);

    <T, R> R accept(ElementVisitor<T, R> visitor, T param);

    interface Builder {
        void setModifiers(int modifiers);

        void setIndex(int index);
    }
}
