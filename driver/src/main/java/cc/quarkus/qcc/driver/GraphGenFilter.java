package cc.quarkus.qcc.driver;

import cc.quarkus.qcc.type.definition.element.Element;

@FunctionalInterface
public interface GraphGenFilter {
    boolean accept(Element element, Phase phase);
}
