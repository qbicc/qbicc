package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;

abstract class AbstractBufferBacked implements BufferBacked {
    final ByteBuffer buffer;

    AbstractBufferBacked(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBackingBuffer() {
        return buffer;
    }

    public int getByte(int index) {
        return buffer.get(index) & 0xff;
    }

    public int getShort(int index) {
        return buffer.getShort(index) & 0xffff;
    }

    public int getInt(int index) {
        return buffer.getInt(index);
    }

    public long getLong(int index) {
        return buffer.getLong(index);
    }

    public boolean utf8TextEquals(int offset, int length, final String str) {
        int strLength = str.length();
        int j = 0;
        int i = 0;
        for (; i < length && j < strLength; i ++, j ++) {
            int a = getByte(offset + i);
            if (a < 0x80) {
                if (str.charAt(j) != (char) a) {
                    return false;
                }
            } else if (a < 0xC0) {
                return false;
            } else {
                int b = getByte(offset + i + 1);
                if (b < 0x80 || b >= 0xC0) {
                    return false;
                } else if (a < 0xE0) {
                    i ++; // eat up extra byte
                    if (str.charAt(j) != (char) ((a & 0b11111) << 6 | b & 0b111111)) {
                        return false;
                    }
                } else {
                    int c = getByte(offset + i + 2);
                    if (c < 0x80 || c >= 0xC0) {
                        return false;
                    } else if (a < 0xF0) {
                        i += 2; // eat up extra two bytes
                        if (str.charAt(j) != (char) ((a & 0b1111) << 12 | (b & 0b111111) << 6 | c & 0b111111)) {
                            return false;
                        }
                    } else {
                        throw new IllegalStateException("Invalid Modified UTF-8 sequence in class file");
                    }
                }
            }
        }
        return i == length && j == strLength;
    }

    public String getUtf8Text(int offset, int length, StringBuilder scratch) {
        scratch.setLength(0);
        for (int i = 0; i < length; i ++) {
            int a = getByte(offset + i);
            if (a < 0x80) {
                scratch.append((char) a);
            } else if (a < 0xC0) {
                scratch.append('�');
            } else {
                int b = getByte(offset + 2);
                if (b < 0x80 || b >= 0xC0) {
                    scratch.append('�');
                } else if (a < 0xE0) {
                    i ++; // eat up extra byte
                    scratch.append((char) ((a & 0b11111) << 6 | b & 0b111111));
                } else {
                    int c = getByte(offset + 3);
                    if (c < 0x80 || c >= 0xC0) {
                        scratch.append('�');
                    } else if (a < 0xF0) {
                        i += 2; // eat up extra two bytes
                        scratch.append((char) ((a & 0b1111) << 12 | (b & 0b111111) << 6 | c & 0b111111));
                    } else {
                        throw new IllegalStateException("Invalid Modified UTF-8 sequence in class file");
                    }
                }
            }
        }
        return scratch.toString();
    }

    public ByteBuffer slice(int offset, int length) {
        return buffer.duplicate().position(offset).limit(offset + length).slice();
    }
}
