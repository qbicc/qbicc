package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.FuncType;

/**
 *
 */
public
interface Encoder {
    int encode(BranchTarget branchTarget);

    int encode(Element element);

    int encode(Func func);

    int encode(FuncType type);

    int encode(Global global);

    int encode(Local local);

    int encode(Memory memory);

    int encode(Table table);

    int encode(Segment seg);

    int encode(Tag tag);

    interface Delegating extends Encoder {
        Encoder delegate();

        @Override
        default int encode(BranchTarget branchTarget) {
            return delegate().encode(branchTarget);
        }

        @Override
        default int encode(Element element) {
            return delegate().encode(element);
        }

        @Override
        default int encode(Func func) {
            return delegate().encode(func);
        }

        @Override
        default int encode(FuncType type) {
            return delegate().encode(type);
        }

        @Override
        default int encode(Global global) {
            return delegate().encode(global);
        }

        @Override
        default int encode(Local local) {
            return delegate().encode(local);
        }

        @Override
        default int encode(Memory memory) {
            return delegate().encode(memory);
        }

        @Override
        default int encode(Table table) {
            return delegate().encode(table);
        }

        @Override
        default int encode(Segment seg) {
            return delegate().encode(seg);
        }

        @Override
        default int encode(Tag tag) {
            return delegate().encode(tag);
        }
    }
}
