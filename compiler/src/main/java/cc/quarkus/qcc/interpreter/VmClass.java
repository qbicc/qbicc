package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

/**
 *
 */
public interface VmClass extends VmObject {

    ValidatedTypeDefinition getTypeDefinition();
}
