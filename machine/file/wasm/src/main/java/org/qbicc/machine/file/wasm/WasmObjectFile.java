package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.Section;

/**
 * A Wasm object file.
 */
public final class WasmObjectFile implements ObjectFile {
    @Override
    public int getSymbolValueAsByte(String name) {
        return 0;
    }

    @Override
    public int getSymbolValueAsInt(String name) {
        return 0;
    }

    @Override
    public long getSymbolValueAsLong(String name) {
        return 0;
    }

    @Override
    public byte[] getSymbolAsBytes(String name, int size) {
        return new byte[0];
    }

    @Override
    public String getSymbolValueAsUtfString(String name, int nbytes) {
        return null;
    }

    @Override
    public long getSymbolSize(String name) {
        return 0;
    }

    @Override
    public ByteOrder getByteOrder() {
        return null;
    }

    @Override
    public Cpu getCpu() {
        return null;
    }

    @Override
    public ObjectType getObjectType() {
        return null;
    }

    @Override
    public Section getSection(String name) {
        return null;
    }

    @Override
    public String getRelocationSymbolForSymbolValue(String symbol) {
        return null;
    }

    @Override
    public String getStackMapSectionName() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
