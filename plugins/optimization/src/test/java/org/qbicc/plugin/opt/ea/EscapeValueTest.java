package org.qbicc.plugin.opt.ea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.qbicc.plugin.opt.ea.EscapeValue.*;

public class EscapeValueTest {

    @Test
    public void testMergetToNoEscape() {
        assertEquals(NO_ESCAPE, NO_ESCAPE.merge(NO_ESCAPE));
    }

    @Test
    public void testMergeToArgEscape() {
        assertEquals(ARG_ESCAPE, NO_ESCAPE.merge(ARG_ESCAPE));
        assertEquals(ARG_ESCAPE, ARG_ESCAPE.merge(NO_ESCAPE));
        assertEquals(ARG_ESCAPE, ARG_ESCAPE.merge(ARG_ESCAPE));
    }

    @Test
    public void testMergeToGlobalEscape() {
        assertEquals(GLOBAL_ESCAPE, GLOBAL_ESCAPE.merge(NO_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, GLOBAL_ESCAPE.merge(ARG_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, GLOBAL_ESCAPE.merge(GLOBAL_ESCAPE));

        assertEquals(GLOBAL_ESCAPE, NO_ESCAPE.merge(GLOBAL_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, ARG_ESCAPE.merge(GLOBAL_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, GLOBAL_ESCAPE.merge(GLOBAL_ESCAPE));
    }

}
