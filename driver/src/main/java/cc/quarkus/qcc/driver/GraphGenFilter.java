package org.qbicc.driver;

import org.qbicc.type.definition.element.Element;

@FunctionalInterface
public interface GraphGenFilter {
    boolean accept(Element element, Phase phase);
}
