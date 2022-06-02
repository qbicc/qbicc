package org.qbicc.machine.file.wasm;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.kaitai.struct.KaitaiStruct;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.wasm.kaitai.VlqBase128Le;
import org.qbicc.machine.file.wasm.kaitai.Webassembly;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.Section;

/**
 * A Wasm object file.
 */
public final class WasmObjectFile implements ObjectFile {

    private final Map<String, Integer> sizes = new HashMap<>();

    public WasmObjectFile(Path path) throws IOException {
        Webassembly webassembly = Webassembly.fromFile(path.toString());
        HashMap<Integer, String> indexSymbol = new HashMap<>();
        for (Webassembly.Section sect : webassembly.sections().sections()) {
            KaitaiStruct struct = sect.payloadData();

            if (struct instanceof Webassembly.UnimplementedSection) {
                List<Webassembly.LinkingCustomType> linking = ((Webassembly.UnimplementedSection) struct).linking();
                if (linking == null) continue;

                for (Webassembly.LinkingCustomType linkingCustomType : linking) {
                    for (Webassembly.LinkingCustomSubsectionType subsection : linkingCustomType.subsections()) {
                        if (subsection.type() != 8) continue;

                        for (Webassembly.SyminfoType info : subsection.symbolTable().infos()) {
                            if (info.kind() != Webassembly.Symtab.DATA) continue;
                            Webassembly.SyminfoData data = info.data();
                            String name = data.nameData();
                            VlqBase128Le idx = data.index();
                            if (idx == null) {
                                continue;
                            }
                            Integer index = idx.value();
                            indexSymbol.put(index, name);
                        }
                    }
                }
            }
        }
        for (Webassembly.Section sect : webassembly.sections().sections()) {
            KaitaiStruct struct = sect.payloadData();
            if (struct instanceof Webassembly.DataSection) {
                ArrayList<Webassembly.DataSegmentType> entries = ((Webassembly.DataSection) struct).entries();
                for (int i = 0; i < entries.size(); i++) {
                    Webassembly.DataSegmentType data = entries.get(i);
                    // data is byte-sized, but the parser uses Integers
                    ArrayList<Integer> bytes = data.data();
                    int acc = 0;
                    for (int j = 0; j < bytes.size(); j++) {
                        int v = bytes.get(j);
                        acc |= v << ( j * 8 );
                    }
                    sizes.put(indexSymbol.get(i), acc);
                }
            }
        }
    }

    @Override
    public int getSymbolValueAsByte(String name) {
        return getSize(name);
    }

    @Override
    public int getSymbolValueAsInt(String name) {
        return getSize(name);
    }

    @Override
    public long getSymbolValueAsLong(String name) {
        return getSize(name);
    }

    @Override
    public byte[] getSymbolAsBytes(String name, int size) {
        return new byte[] { getSize(name).byteValue() };
    }

    @Override
    public String getSymbolValueAsUtfString(String name, int nbytes) {
        return new String(getSymbolAsBytes(name, nbytes), StandardCharsets.UTF_8);
    }

    private Integer getSize(String name) {
        return sizes.getOrDefault(name, 0);
    }

    @Override
    public long getSymbolSize(String name) {
        return getSize(name);
    }

    @Override
    public ByteOrder getByteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public Cpu getCpu() {
        return Cpu.WASM32;
    }

    @Override
    public ObjectType getObjectType() {
        return ObjectType.WASM;
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
        return "__llvm_stackmaps";
    }

    @Override
    public void close() throws IOException {

    }
}
