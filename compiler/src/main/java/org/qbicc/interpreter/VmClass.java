package org.qbicc.interpreter;

import org.qbicc.type.definition.ValidatedTypeDefinition;

/**
 *
 */
public interface VmClass extends VmObject {

    ValidatedTypeDefinition getTypeDefinition();
}
