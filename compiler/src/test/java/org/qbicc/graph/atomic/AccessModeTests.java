package org.qbicc.graph.atomic;

import static org.junit.jupiter.api.Assertions.*;
import static org.qbicc.graph.atomic.AccessModes.*;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class AccessModeTests {

    @Test
    public void testCombinedWithUpgrade() {
        assertSame(SinglePlain, SingleUnshared.combinedWith(SinglePlain));
        assertSame(SingleOpaque, SinglePlain.combinedWith(SingleOpaque));
        assertSame(SingleAcquire, SingleOpaque.combinedWith(SingleAcquire));
        assertSame(SingleRelease, SingleOpaque.combinedWith(SingleRelease));
        assertSame(SingleAcqRel, SingleAcquire.combinedWith(SingleRelease));

        assertSame(GlobalPlain, GlobalUnshared.combinedWith(GlobalPlain));
        assertSame(GlobalAcquire, GlobalLoadStore.combinedWith(GlobalLoadLoad));
        assertSame(GlobalRelease, GlobalLoadStore.combinedWith(GlobalStoreStore));
        assertSame(GlobalAcquire, GlobalPlain.combinedWith(GlobalAcquire));
        assertSame(GlobalRelease, GlobalPlain.combinedWith(GlobalRelease));
        assertSame(GlobalAcqRel, GlobalAcquire.combinedWith(GlobalRelease));
        assertSame(GlobalSeqCst, GlobalAcqRel.combinedWith(GlobalStoreLoad));
        assertSame(GlobalSeqCst, GlobalLoadLoad.combinedWith(GlobalStoreLoad));
        assertSame(GlobalSeqCst, GlobalLoadStore.combinedWith(GlobalStoreLoad));
        assertSame(GlobalSeqCst, GlobalStoreStore.combinedWith(GlobalStoreLoad));
    }
}
