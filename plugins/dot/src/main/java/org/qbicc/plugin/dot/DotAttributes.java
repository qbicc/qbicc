package org.qbicc.plugin.dot;

public interface DotAttributes {
    String color();
    String style();

    /**
     * <a href="https://graphviz.org/docs/attr-types/portPos">Port position</a>.
     * Modifier indicating where on a node an edge should be aimed.
     */
    String portPos();
}
