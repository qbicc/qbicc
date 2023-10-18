package org.qbicc.graph;

import java.util.Objects;

public final class BlockLabel {
    private Object target;

    public BlockLabel() {
    }

    public BasicBlock setTarget(BasicBlock target) {
        if (target.getHandleIfExists() == null) {
            target.setHandle(this);
        }
        this.target = target;
        return target;
    }

    public BlockLabel setTarget(BlockLabel target) {
        if (target == this) {
            return target;
        }
        Object oldTarget = this.target;
        if (oldTarget instanceof BlockLabel) {
            ((BlockLabel) oldTarget).setTarget(target);
        } else {
            this.target = target;
        }
        return target;
    }

    public BlockLabel lastHandle() {
        Object target = this.target;
        if (target instanceof BlockLabel) {
            return ((BlockLabel) target).lastHandle();
        } else if (target == null || target instanceof BasicBlock) {
            return this;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean hasTarget() {
        return lastHandle().target instanceof BasicBlock;
    }

    // helper
    public static BlockLabel of(BasicBlock node) {
        return node == null ? null : node.getHandle().lastHandle();
    }

    public static BasicBlock getTargetOf(BlockLabel handle) {
        return handle == null ? null : (BasicBlock) handle.lastHandle().target;
    }

    public static BasicBlock requireTargetOf(final BlockLabel label) {
        return Objects.requireNonNull(getTargetOf(label), "Unresolved block label");
    }

    public String toString() {
        Object target = this.target;
        if (target == null) {
            return "empty";
        } else if (target instanceof BlockLabel) {
            return "fwd to " + target;
        } else {
            return "-> " + target;
        }
    }
}
