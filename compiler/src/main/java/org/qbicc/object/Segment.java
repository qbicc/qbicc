package org.qbicc.object;

/**
 * A segment into which sections may be defined.
 */
public enum Segment {
    /**
     * The so-called "text" segment, where read-only program information is loaded.
     */
    TEXT("text"),
    /**
     * The so-called "data" segment, where read-write program information is loaded.
     */
    DATA("data"),
    ;

    private final String str;

    Segment(final String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
