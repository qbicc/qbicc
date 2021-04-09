package org.qbicc.type.definition.classfile;

import java.nio.ByteBuffer;

/**
 *
 */
public interface BufferBacked {
    ByteBuffer getBackingBuffer();

    int getByte(int index);

    int getShort(int index);

    int getInt(int index);

    long getLong(int index);

    boolean utf8TextEquals(int offset, int length, String str);

    String getUtf8Text(int offset, int length, StringBuilder scratch);

    ByteBuffer slice(int offset, int length);
}
