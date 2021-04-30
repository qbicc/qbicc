package org.qbicc.machine.object;

import java.nio.ByteBuffer;

/**
 * An object file section.
 */
public interface Section {
    String getName();

    ByteBuffer getSectionContent();
}
