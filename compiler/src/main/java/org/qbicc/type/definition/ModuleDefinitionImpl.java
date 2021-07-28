package org.qbicc.type.definition;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.qbicc.type.definition.classfile.ClassFile;

final class ModuleDefinitionImpl implements ModuleDefinition {
    final String name;
    final String version;
    final int modifiers;
    final String[] requiresNames;
    final int[] requiresModifiers;
    final String[] requiresVersions;
    final String[] exportsNames;
    final int[] exportsModifiers;
    final String[][] exportsTo;
    final String[] opensNames;
    final int[] opensModifiers;
    final String[][] opensTo;
    final String[] uses;
    final String[] provides;
    final String[][] providesWith;
    final String[] packages;

    private static final String[] NO_STRINGS = new String[0];
    private static final String[][] NO_STRING_ARRAYS = new String[0][];
    private static final int[] NO_INTS = new int[0];

    ModuleDefinitionImpl(final ByteBuffer orig) {
        // this part could probably be factored into a common method of some sort
        if (orig.order() != ByteOrder.BIG_ENDIAN) {
            throw new DefineFailedException("Wrong byte buffer order");
        }
        ByteBuffer buffer = orig.duplicate();
        int magic = buffer.getInt();
        if (magic != 0xcafebabe) {
            throw new DefineFailedException("Bad magic number");
        }
        int minor = buffer.getShort() & 0xffff;
        int major = buffer.getShort() & 0xffff;
        // todo fix up
        if (major < 45 || major == 45 && minor < 3 || major > 55 || major == 55 && minor > 0) {
            throw new DefineFailedException("Unsupported class version " + major + "." + minor);
        }
        int cpCount = (buffer.getShort() & 0xffff) - 1;
        // one extra slot because the constant pool is one-based, so just leave a hole at the beginning
        int[] cpOffsets = new int[cpCount + 1];
        String[] strCache = new String[cpCount + 1];
        for (int i = 1; i < cpCount + 1; i ++) {
            cpOffsets[i] = buffer.position();
            int tag = buffer.get() & 0xff;
            switch (tag) {
                case ClassFileUtil.CONSTANT_Utf8: {
                    int size = buffer.getShort() & 0xffff;
                    buffer.position(buffer.position() + size);
                    break;
                }
                case ClassFileUtil.CONSTANT_Integer:
                case ClassFileUtil.CONSTANT_Float:
                case ClassFileUtil.CONSTANT_Fieldref:
                case ClassFileUtil.CONSTANT_Methodref:
                case ClassFileUtil.CONSTANT_InterfaceMethodref:
                case ClassFileUtil.CONSTANT_NameAndType:
                case ClassFileUtil.CONSTANT_Dynamic:
                case ClassFileUtil.CONSTANT_InvokeDynamic: {
                    buffer.position(buffer.position() + 4);
                    break;
                }
                case ClassFileUtil.CONSTANT_Class:
                case ClassFileUtil.CONSTANT_String:
                case ClassFileUtil.CONSTANT_MethodType:
                case ClassFileUtil.CONSTANT_Module:
                case ClassFileUtil.CONSTANT_Package: {
                    buffer.position(buffer.position() + 2);
                    break;
                }
                case ClassFileUtil.CONSTANT_Long:
                case ClassFileUtil.CONSTANT_Double: {
                    buffer.position(buffer.position() + 8);
                    i++; // two slots
                    break;
                }
                case ClassFileUtil.CONSTANT_MethodHandle: {
                    buffer.position(buffer.position() + 3);
                    break;
                }
                default: {
                    throw new DefineFailedException("Unknown constant pool tag " + Integer.toHexString(tag) + " at index " + i);
                }
            }
        }
        StringBuilder b = new StringBuilder(64);
        int access = buffer.getShort() & 0xffff;
        if (access != ClassFile.ACC_MODULE) {
            throw new DefineFailedException("Expected only a ACC_MODULE flag");
        }
        int thisClassIdx = buffer.getShort() & 0xffff;
        if (! ClassFileUtil.classNameEquals(buffer, thisClassIdx, cpOffsets, "module-info")) {
            throw new DefineFailedException("Class name mismatch");
        }
        if (buffer.getShort() != 0) {
            throw new DefineFailedException("Module info files should have no super class");
        }
        if (buffer.getShort() != 0) {
            throw new DefineFailedException("Module info files should have no interfaces");
        }
        if (buffer.getShort() != 0) {
            throw new DefineFailedException("Module info files should have no fields");
        }
        if (buffer.getShort() != 0) {
            throw new DefineFailedException("Module info files should have no methods");
        }
        // attributes
        String name = null;
        String version = null;
        int modifiers = 0;
        String[] requiresNames = NO_STRINGS;
        int[] requiresModifiers = NO_INTS;
        String[] requiresVersions = NO_STRINGS;
        String[] exportsNames = NO_STRINGS;
        int[] exportsModifiers = NO_INTS;
        String[][] exportsTo = NO_STRING_ARRAYS;
        String[] opensNames = NO_STRINGS;
        int[] opensModifiers = NO_INTS;
        String[][] opensTo = NO_STRING_ARRAYS;
        String[] uses = NO_STRINGS;
        String[] provides = NO_STRINGS;
        String[][] providesWith = NO_STRING_ARRAYS;
        String[] packages = NO_STRINGS;

        int attrCnt = buffer.getShort() & 0xffff;
        for (int i = 0; i < attrCnt; i ++) {
            int attrNameIdx = buffer.getShort() & 0xffff;
            int size = buffer.getInt();
            int endPos = buffer.position() + size;
            if (ClassFileUtil.utf8EntryEquals(buffer, cpOffsets[attrNameIdx], "ModulePackages")) {
                int packageCnt = buffer.getShort() & 0xffff;
                if (packageCnt > 0) {
                    packages = new String[packageCnt];
                    for (int j = 0; j < packageCnt; j++) {
                        packages[j] = ClassFileUtil.getPackageName(buffer, buffer.getShort() & 0xffff, cpOffsets, strCache, b);
                    }
                }
            } else if (ClassFileUtil.utf8EntryEquals(buffer, cpOffsets[attrNameIdx], "Module")) {
                int moduleNameIdx = buffer.getShort() & 0xffff;
                name = ClassFileUtil.getModuleName(buffer, moduleNameIdx, cpOffsets, strCache, b);
                modifiers = buffer.getShort() & 0xffff;
                int versionIdx = buffer.getShort() & 0xffff;
                version = ClassFileUtil.getUtf8Entry(buffer, versionIdx, cpOffsets, strCache, b);
                int requiresCnt = buffer.getShort() & 0xffff;
                if (requiresCnt > 0) {
                    requiresNames = new String[requiresCnt];
                    requiresModifiers = new int[requiresCnt];
                    requiresVersions = new String[requiresCnt];
                    for (int j = 0; j < requiresCnt; j++) {
                        int reqIdx = buffer.getShort() & 0xffff;
                        requiresNames[j] = ClassFileUtil.getModuleName(buffer, reqIdx, cpOffsets, strCache, b);
                        requiresModifiers[j] = buffer.getShort() & 0xffff;
                        int reqVerIdx = buffer.getShort() & 0xffff;
                        if (reqVerIdx != 0) {
                            requiresVersions[j] = ClassFileUtil.getUtf8Entry(buffer, reqVerIdx, cpOffsets, strCache, b);
                        }
                    }
                }
                int exportsCnt = buffer.getShort() & 0xffff;
                if (exportsCnt > 0) {
                    exportsNames = new String[exportsCnt];
                    exportsModifiers = new int[exportsCnt];
                    exportsTo = new String[exportsCnt][];
                    for (int j = 0; j < exportsCnt; j ++) {
                        int expIdx = buffer.getShort() & 0xffff;
                        exportsNames[j] = ClassFileUtil.getPackageName(buffer, expIdx, cpOffsets, strCache, b);
                        exportsModifiers[j]= buffer.getShort() & 0xffff;
                        int expToCnt = buffer.getShort() & 0xffff;
                        if (expToCnt == 0) {
                            exportsTo[j] = NO_STRINGS;
                        } else {
                            exportsTo[j] = new String[expToCnt];
                            for (int k = 0; k < expToCnt; k ++) {
                                int expToIdx = buffer.getShort() & 0xffff;
                                exportsTo[j][k] = ClassFileUtil.getModuleName(buffer, expToIdx, cpOffsets, strCache, b);
                            }
                        }
                    }
                }
                int opensCnt = buffer.getShort() & 0xffff;
                if (opensCnt > 0) {
                    opensNames = new String[opensCnt];
                    opensModifiers = new int[opensCnt];
                    opensTo = new String[opensCnt][];
                    for (int j = 0; j < opensCnt; j ++) {
                        int openIdx = buffer.getShort() & 0xffff;
                        opensNames[j] = ClassFileUtil.getPackageName(buffer, openIdx, cpOffsets, strCache, b);
                        opensModifiers[j]= buffer.getShort() & 0xffff;
                        int openToCnt = buffer.getShort() & 0xffff;
                        if (openToCnt == 0) {
                            opensTo[j] = NO_STRINGS;
                        } else {
                            opensTo[j] = new String[openToCnt];
                            for (int k = 0; k < openToCnt; k ++) {
                                int openToIdx = buffer.getShort() & 0xffff;
                                opensTo[j][k] = ClassFileUtil.getModuleName(buffer, openToIdx, cpOffsets, strCache, b);
                            }
                        }
                    }
                }
                int usesCnt = buffer.getShort() & 0xffff;
                if (usesCnt > 0) {
                    uses = new String[usesCnt];
                    for (int j = 0; j < usesCnt; j ++) {
                        int usesIdx = buffer.getShort() & 0xffff;
                        uses[j] = ClassFileUtil.getClassName(buffer, usesIdx, cpOffsets, strCache, b);
                    }
                }
                int providesCnt = buffer.getShort() & 0xffff;
                if (providesCnt > 0) {
                    provides = new String[providesCnt];
                    providesWith = new String[providesCnt][];
                    for (int j = 0; j < providesCnt; j ++) {
                        int provIdx = buffer.getShort() & 0xffff;
                        provides[j] = ClassFileUtil.getClassName(buffer, provIdx, cpOffsets, strCache, b);
                        int provWithCnt = buffer.getShort() & 0xffff;
                        if (provWithCnt == 0) {
                            providesWith[j] = NO_STRINGS;
                        } else {
                            providesWith[j] = new String[provWithCnt];
                            for (int k = 0; k < provWithCnt; k ++) {
                                int provWithIdx = buffer.getShort() & 0xffff;
                                providesWith[j][k] = ClassFileUtil.getClassName(buffer, provWithIdx, cpOffsets, strCache, b);
                            }
                        }
                    }
                }
            } else {
                // skip
                buffer.position(endPos);
            }
            if (buffer.position() != endPos) {
                throw new DefineFailedException("Malformed attribute at index " + i);
            }
        }
        if (buffer.hasRemaining()) {
            throw new DefineFailedException("Extra data at end of class file");
        }
        this.name = name;
        this.version = version;
        this.modifiers = modifiers;
        this.requiresNames = requiresNames;
        this.requiresModifiers = requiresModifiers;
        this.requiresVersions = requiresVersions;
        this.exportsNames = exportsNames;
        this.exportsModifiers = exportsModifiers;
        this.exportsTo = exportsTo;
        this.opensNames = opensNames;
        this.opensModifiers = opensModifiers;
        this.opensTo = opensTo;
        this.uses = uses;
        this.provides = provides;
        this.providesWith = providesWith;
        this.packages = packages;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public int getModifiers() {
        return modifiers;
    }

    public int getPackageCount() {
        return packages.length;
    }

    public String getPackage(final int index) throws IndexOutOfBoundsException {
        return packages[index];
    }

    public int getRequiresCount() {
        return requiresNames.length;
    }

    public String getRequiresName(final int index) throws IndexOutOfBoundsException {
        return requiresNames[index];
    }

    public int getRequiresModifiers(final int index) throws IndexOutOfBoundsException {
        return requiresModifiers[index];
    }

    public String getRequiresVersion(final int index) throws IndexOutOfBoundsException {
        return requiresVersions[index];
    }

    public int getExportsCount() {
        return exportsNames.length;
    }

    public String getExportsPackageName(final int index) throws IndexOutOfBoundsException {
        return exportsNames[index];
    }

    public int getExportsModifiers(final int index) throws IndexOutOfBoundsException {
        return exportsModifiers[index];
    }

    public int getExportsToCount(final int index) throws IndexOutOfBoundsException {
        return exportsTo[index].length;
    }

    public String getExportsTo(final int exportsIndex, final int exportsToIndex) throws IndexOutOfBoundsException {
        return exportsTo[exportsIndex][exportsToIndex];
    }

    public int getOpensCount() {
        return opensNames.length;
    }

    public String getOpensPackageName(final int index) throws IndexOutOfBoundsException {
        return opensNames[index];
    }

    public int getOpensModifiers(final int index) throws IndexOutOfBoundsException {
        return opensModifiers[index];
    }

    public int getOpensToCount(final int index) throws IndexOutOfBoundsException {
        return opensTo[index].length;
    }

    public String getOpensTo(final int opensIndex, final int opensToIndex) throws IndexOutOfBoundsException {
        return opensTo[opensIndex][opensToIndex];
    }

    public int getUsesCount() {
        return uses.length;
    }

    public String getUses(final int index) throws IndexOutOfBoundsException {
        return uses[index];
    }

    public int getProvidesCount() {
        return provides.length;
    }

    public String getProvides(final int index) throws IndexOutOfBoundsException {
        return provides[index];
    }

    public int getProvidesWithCount(final int index) {
        return providesWith[index].length;
    }

    public String getProvidesWith(final int providesIndex, final int providesWithIndex) throws IndexOutOfBoundsException {
        return providesWith[providesIndex][providesWithIndex];
    }
}
