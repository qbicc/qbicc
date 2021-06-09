package org.qbicc.runtime.deserialization;

public interface SerializationConstants {
    byte NULL = 0x00;

    byte TAG_MASK = (byte)0xF0;

    // A new object: 0x1G
    byte OBJECT_TAG = 0x10;
    byte OBJECT = OBJECT_TAG | 0x01; // 2 byte typeid + instance fields
    byte OBJECT_LONG_LIVED = OBJECT_TAG | 0x02; // 2 byte typeid + instance fields
    byte OBJECT_IMMORTAL = OBJECT_TAG | 0x03; // 2 byte typeid + instance fields

    // A String literal: 0x2S
    byte STRING_TAG = 0x20;
    byte STRING_SMALL_L1 = STRING_TAG | 0x01;  // 1 byte length + data
    byte STRING_LARGE_L1 = STRING_TAG | 0x02;  // 4 byte length + data
    byte STRING_SMALL_U16 = STRING_TAG | 0x03; // 1 byte length + data
    byte STRING_LARGE_U16 = STRING_TAG | 0x04; // 4 byte length + data

    // Arrays: are either 0x3T (1 byte length) or 0x4T (4 byte length)
    byte ARRAY_SMALL_TAG = 0x30;
    byte ARRAY_SMALL_BOOLEAN = ARRAY_SMALL_TAG | 0x01; // 1 byte length + data
    byte ARRAY_SMALL_BYTE = ARRAY_SMALL_TAG | 0x02; // 1 byte length + data
    byte ARRAY_SMALL_SHORT = ARRAY_SMALL_TAG | 0x03; // 1 byte length + data
    byte ARRAY_SMALL_CHAR = ARRAY_SMALL_TAG | 0x04; // 1 byte length + data
    byte ARRAY_SMALL_INT = ARRAY_SMALL_TAG | 0x05; // 1 byte length + data
    byte ARRAY_SMALL_FLOAT = ARRAY_SMALL_TAG | 0x06; // 1 byte length + data
    byte ARRAY_SMALL_LONG = ARRAY_SMALL_TAG | 0x07; // 1 byte length + data
    byte ARRAY_SMALL_DOUBLE = ARRAY_SMALL_TAG | 0x08; // 1 byte length + data
    byte ARRAY_SMALL_OBJECT = ARRAY_SMALL_TAG | 0x09; // 1 byte length + elements
    byte ARRAY_SMALL_STRING = ARRAY_SMALL_TAG | 0x0A; // 1 byte length + elements
    byte ARRAY_SMALL_CLASS = ARRAY_SMALL_TAG | 0x0B; // 1 byte length + 2 byte typeid + elements

    // Arrays are 0x3T (1 byte length) or 0x4T (4 byte length)
    byte ARRAY_LARGE_TAG = 0x40;
    byte ARRAY_LARGE_BOOLEAN = ARRAY_LARGE_TAG | 0x01; // 4 byte length + data
    byte ARRAY_LARGE_BYTE = ARRAY_LARGE_TAG | 0x02; // 4 byte length + data
    byte ARRAY_LARGE_SHORT = ARRAY_LARGE_TAG | 0x03; // 4 byte length + data
    byte ARRAY_LARGE_CHAR = ARRAY_LARGE_TAG | 0x04; // 4 byte length + data
    byte ARRAY_LARGE_INT = ARRAY_LARGE_TAG | 0x05; // 4 byte length + data
    byte ARRAY_LARGE_FLOAT = ARRAY_LARGE_TAG | 0x06; // 4 byte length + data
    byte ARRAY_LARGE_LONG = ARRAY_LARGE_TAG | 0x07; // 4 byte length + data
    byte ARRAY_LARGE_DOUBLE = ARRAY_LARGE_TAG | 0x08; // 4 byte length + data
    byte ARRAY_LARGE_OBJECT = ARRAY_LARGE_TAG | 0x09; // 4 byte length + elements
    byte ARRAY_LARGE_STRING = ARRAY_LARGE_TAG | 0x0A; // 4 byte length + elements
    byte ARRAY_LARGE_CLASS = ARRAY_LARGE_TAG | 0x0B; // 4 byte length + 2 byte typeid + elements

    // A class literal
    byte CLASS_LITERAL_TAG = 0x50;

    // A non-tiny backref
    byte BACKREF_TAG = 0x70;
    byte BACKREF_SMALL = BACKREF_TAG | 0x01; // followed by 2 byte backref
    byte BACKREF_LARGE = BACKREF_TAG | 0x03; // followed by 4 byte backref

    // A Tiny backref can encode 1 to 127 objects back in the serialization buffer
    // We reserve the top bit of the TAG to indicate the other 7 bits are a tiny backref
    byte TINY_REF_TAG_BIT = (byte)0x80;
    byte TINY_REF_MASK = 0x7F;
}
