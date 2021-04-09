package org.qbicc.graph.schedule;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
final class Loop extends BitSet {
    final int number;
    final Loop parent;
    final int exit;
    final Set<Loop> children = new HashSet<>();

    Loop(final int number, final Loop parent, final int exit) {
        this.number = number;
        this.parent = parent;
        this.exit = exit;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    int getLevel() {
        return parent == null ? 0 : parent.getLevel() + 1;
    }

    int getExit() {
        return exit;
    }

    void addMember(final int index) {
        set(index);
        if (parent != null) {
            parent.addMember(index);
        }
    }
}
