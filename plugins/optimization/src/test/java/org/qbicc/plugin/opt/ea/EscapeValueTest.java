package org.qbicc.plugin.opt.ea;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.qbicc.plugin.opt.ea.EscapeValue.*;

public class EscapeValueTest {

    @Test
    public void testMergeToNoEscape() {
        assertEquals(NO_ESCAPE, EscapeValue.merge(NO_ESCAPE, NO_ESCAPE));
    }

    @Test
    public void testMergeToArgEscape() {
        assertEquals(ARG_ESCAPE, EscapeValue.merge(NO_ESCAPE, ARG_ESCAPE));
        assertEquals(ARG_ESCAPE, EscapeValue.merge(ARG_ESCAPE, NO_ESCAPE));
        assertEquals(ARG_ESCAPE, EscapeValue.merge(ARG_ESCAPE, ARG_ESCAPE));
    }

    @Test
    public void testMergeToGlobalEscape() {
        assertEquals(GLOBAL_ESCAPE, EscapeValue.merge(GLOBAL_ESCAPE, NO_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, EscapeValue.merge(GLOBAL_ESCAPE, ARG_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, EscapeValue.merge(GLOBAL_ESCAPE, GLOBAL_ESCAPE));

        assertEquals(GLOBAL_ESCAPE, EscapeValue.merge(NO_ESCAPE, GLOBAL_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, EscapeValue.merge(ARG_ESCAPE, GLOBAL_ESCAPE));
        assertEquals(GLOBAL_ESCAPE, EscapeValue.merge(GLOBAL_ESCAPE, GLOBAL_ESCAPE));
    }

}
