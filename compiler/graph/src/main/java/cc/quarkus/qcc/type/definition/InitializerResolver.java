package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.InitializerElement;

/**
 *
 */
public interface InitializerResolver {
    InitializerResolver EMPTY = new InitializerResolver() {
        public InitializerElement resolveInitializer(final int index) {
            return InitializerElement.EMPTY;
        }
    };

    InitializerElement resolveInitializer(int index);
}
