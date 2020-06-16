package cc.quarkus.qcc.type.definition;

import java.nio.ByteBuffer;

/**
 * Class file constants and utilities.
 */
public final class ClassFile {
    private ClassFile() {}

    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5; // Double size!
    public static final int CONSTANT_Double = 6; // Double size!
    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_NameAndType = 12;
    public static final int CONSTANT_MethodHandle = 15;
    public static final int CONSTANT_MethodType = 16;
    public static final int CONSTANT_Dynamic = 17;
    public static final int CONSTANT_InvokeDynamic = 18;
    public static final int CONSTANT_Module = 19;
    public static final int CONSTANT_Package = 20;

    public static final int ACC_PUBLIC = 1 << 0;
    public static final int ACC_PRIVATE = 1 << 1;
    public static final int ACC_PROTECTED = 1 << 2;
    public static final int ACC_STATIC = 1 << 3;
    public static final int ACC_FINAL = 1 << 4;
    public static final int ACC_SYNCHRONIZED = 1 << 5;
    public static final int ACC_SUPER = 1 << 5; // same as ACC_SYNCHRONIZED
    public static final int ACC_BRIDGE = 1 << 6;
    public static final int ACC_VOLATILE = 1 << 6; // same as ACC_BRIDGE
    public static final int ACC_VARARGS = 1 << 7;
    public static final int ACC_TRANSIENT = 1 << 7; // same as ACC_VARARGS
    public static final int ACC_NATIVE = 1 << 8;
    public static final int ACC_INTERFACE = 1 << 9;
    public static final int ACC_ABSTRACT = 1 << 10;
    public static final int ACC_STRICT = 1 << 11;
    public static final int ACC_SYNTHETIC = 1 << 12;
    public static final int ACC_ANNOTATION = 1 << 13;
    public static final int ACC_ENUM = 1 << 14;
    public static final int ACC_MODULE = 1 << 15;

    public static String getClassName(ByteBuffer classFile, int cpEntryNumber, final int[] cpOffsets, StringBuilder scratch) {
        if (cpEntryNumber == 0) {
            return null;
        }
        if (classFile.get(cpOffsets[cpEntryNumber]) != ClassFile.CONSTANT_Class) {
            throw new IllegalStateException("Invalid constant pool entry type");
        }
        int strEntry = classFile.getShort(cpOffsets[cpEntryNumber] + 1) & 0xffff;
        return getUtf8Entry(classFile, cpOffsets[strEntry], scratch);
    }

    public static String getUtf8Entry(ByteBuffer classFile, int cpEntryOffset, StringBuilder scratch) {
        if (classFile.get(cpEntryOffset) != CONSTANT_Utf8) {
            throw new IllegalStateException("Invalid constant pool entry type");
        }
        int length = classFile.getShort(cpEntryOffset + 1) & 0xffff;
        scratch.setLength(0);
        for (int i = 0; i < length; i ++) {
            int a = classFile.get(cpEntryOffset + 3 + i) & 0xff;
            if (a < 0x80) {
                scratch.append((char) a);
            } else if (a < 0xC0) {
                scratch.append('�');
            } else {
                int b = classFile.get(cpEntryOffset + 4 + i) & 0xff;
                if (b < 0x80 || b >= 0xC0) {
                    scratch.append('�');
                } else if (a < 0xE0) {
                    i ++; // eat up extra byte
                    scratch.append((char) ((a & 0b11111) << 6 | b & 0b111111));
                } else {
                    int c = classFile.get(cpEntryOffset + 5 + i) & 0xff;
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

    public static boolean classNameEquals(final ByteBuffer classFile, final int cpEntryNumber, final int[] cpOffsets, final String str) {
        if (classFile.get(cpOffsets[cpEntryNumber]) != ClassFile.CONSTANT_Class) {
            throw new IllegalStateException("Invalid constant pool entry type");
        }
        int strEntry = classFile.getShort(cpOffsets[cpEntryNumber] + 1) & 0xffff;
        return utf8EntryEquals(classFile, cpOffsets[strEntry], str);
    }

    public static boolean stringEquals(final ByteBuffer classFile, final int cpEntryNumber, final int[] cpOffsets, final String str) {
        if (classFile.get(cpOffsets[cpEntryNumber]) != ClassFile.CONSTANT_String) {
            throw new IllegalStateException("Invalid constant pool entry type");
        }
        int strEntry = classFile.getShort(cpOffsets[cpEntryNumber] + 1) & 0xffff;
        return utf8EntryEquals(classFile, cpOffsets[strEntry], str);
    }



    public static boolean utf8EntryEquals(final ByteBuffer classFile, final int cpEntryOffset, final String str) {
        if (classFile.get(cpEntryOffset) != CONSTANT_Utf8) {
            throw new IllegalStateException("Invalid constant pool entry type");
        }
        int length = classFile.getShort(cpEntryOffset + 1) & 0xffff;
        int strLength = str.length();
        int j = 0;
        int i = 0;
        for (; i < length && j < strLength; i ++, j ++) {
            int a = classFile.get(cpEntryOffset + 3 + i) & 0xff;
            if (a < 0x80) {
                if (str.charAt(j) != (char) a) {
                    return false;
                }
            } else if (a < 0xC0) {
                return false;
            } else {
                int b = classFile.get(cpEntryOffset + 4 + i) & 0xff;
                if (b < 0x80 || b >= 0xC0) {
                    return false;
                } else if (a < 0xE0) {
                    i ++; // eat up extra byte
                    if (str.charAt(j) != (char) ((a & 0b11111) << 6 | b & 0b111111)) {
                        return false;
                    }
                } else {
                    int c = classFile.get(cpEntryOffset + 5 + i) & 0xff;
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
}
