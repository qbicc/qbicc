package org.qbicc.interpreter;

import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
public interface VmClass extends VmObject {

    LoadedTypeDefinition getTypeDefinition();
}
