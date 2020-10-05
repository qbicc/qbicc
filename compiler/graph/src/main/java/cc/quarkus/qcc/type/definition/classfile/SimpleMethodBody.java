package cc.quarkus.qcc.type.definition.classfile;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
final class SimpleMethodBody extends VerifiedMethodBody {
    SimpleMethodBody(final DefinedMethodBody dmb, final MethodElement methodElement) {
        super(dmb, methodElement);
    }
}
