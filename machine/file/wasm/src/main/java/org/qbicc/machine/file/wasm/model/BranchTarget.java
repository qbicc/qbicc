package org.qbicc.machine.file.wasm.model;

/**
 * A potential target for a branch instruction.
 */
public sealed interface BranchTarget permits DefinedFunc, BlockInsn {
}
