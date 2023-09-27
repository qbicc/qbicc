package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.FuncType;

/**
 *
 */
public interface Resolver {
    BranchTarget resolveBranchTarget(int index);

    Element resolveElement(int index);

    Func resolveFunc(int index);

    FuncType resolveFuncType(int index);

    Global resolveGlobal(int index);

    Local resolveLocal(int index);

    Memory resolveMemory(int index);

    Table resolveTable(int index);

    Segment resolveSegment(int index);

    Tag resolveTag(int index);

    interface Delegating extends Resolver {
        Resolver getDelegate();

        @Override
        default BranchTarget resolveBranchTarget(int index) {
            return getDelegate().resolveBranchTarget(index);
        }

        @Override
        default Element resolveElement(int index) {
            return getDelegate().resolveElement(index);
        }

        @Override
        default Func resolveFunc(int index) {
            return getDelegate().resolveFunc(index);
        }

        @Override
        default FuncType resolveFuncType(int index) {
            return getDelegate().resolveFuncType(index);
        }

        @Override
        default Global resolveGlobal(int index) {
            return getDelegate().resolveGlobal(index);
        }

        @Override
        default Local resolveLocal(int index) {
            return getDelegate().resolveLocal(index);
        }

        @Override
        default Memory resolveMemory(int index) {
            return getDelegate().resolveMemory(index);
        }

        @Override
        default Table resolveTable(int index) {
            return getDelegate().resolveTable(index);
        }

        @Override
        default Segment resolveSegment(int index) {
            return getDelegate().resolveSegment(index);
        }

        @Override
        default Tag resolveTag(int index) {
            return getDelegate().resolveTag(index);
        }
    }
}
