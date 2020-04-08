package cc.quarkus.qcc.machine.probe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import cc.quarkus.qcc.diagnostic.DiagnosticContext;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.CompilationResult;
import cc.quarkus.qcc.machine.tool.CompilerInvocationBuilder;
import cc.quarkus.qcc.machine.tool.InputSource;
import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;
import cc.quarkus.qcc.machine.file.elf.Elf;
import cc.quarkus.qcc.machine.file.elf.ElfHeader;
import cc.quarkus.qcc.machine.file.elf.ElfSectionHeaderEntry;
import cc.quarkus.qcc.machine.file.elf.ElfSymbolTableEntry;

/**
 *
 */
public class StructProbe {
    private final String name;
    private final Qualifier qualifier;
    private final List<String> preproc = new ArrayList<>();
    private final Map<String, Class<?>> members = new HashMap<>();

    public StructProbe(final Qualifier qualifier, final String name) {
        this.qualifier = qualifier;
        this.name = name;
    }

    public void addMember(String memberName, Class<?> expectedType) {
        members.put(memberName, expectedType);
    }

    public void include(String includeStr) {
        preproc.add("#include " + includeStr);
    }

    public void define(String sym, String value) {
        preproc.add("#define " + sym + " " + value);
    }

    public void define(String sym) {
        preproc.add("#define " + sym);
    }

    public Result runProbe(CCompiler compiler) throws IOException {
        final CompilerInvocationBuilder<?> ib = compiler.invocationBuilder();
        StringBuilder b = new StringBuilder();
        // todo: gnu-specific
        b.append("#define typeof __typeof__\n");
        // todo: C std
        b.append("#include <stddef.h>\n");
        for (String str : preproc) {
            b.append(str).append('\n');
        }
        UnaryOperator<StringBuilder> type;
        if (qualifier == Qualifier.STRUCT) {
            type = struct(name, UnaryOperator.identity());
        } else if (qualifier == Qualifier.UNION) {
            type = union(name, UnaryOperator.identity());
        } else {
            assert qualifier == Qualifier.NONE;
            type = plainType(name, UnaryOperator.identity());
        }
        b.append('\n');
        decl("overall_size", sizeof(type)).apply(b);
        decl("overall_align", alignof(type)).apply(b);
        b.append('\n');
        for (Map.Entry<String, Class<?>> entry : members.entrySet()) {
            final String memberName = entry.getKey();
            decl("sizeof_" + memberName, sizeof(memberOf(type, memberName))).apply(b);
            decl("offsetof_" + memberName, offsetof(type, memberName)).apply(b);
            decl("alignof_" + memberName, alignof(memberOf(type, memberName))).apply(b);
        }
        ib.setInputSource(new InputSource.String(b.toString()));
        final CompilationResult compilationResult = ib.invoke();
        if (DiagnosticContext.errors() > 0) {
            // failed due to errors
            return null;
        }
        final Path path = compilationResult.getObjectFilePath();
        // read back the symbol info
        try (BinaryBuffer objFile = BinaryBuffer.openRead(path)) {
            final ElfHeader elfHeader = ElfHeader.forBuffer(objFile);
            long overallSize = getSymbolValueAsLong(elfHeader, "overall_size");
            long overallAlign = getSymbolValueAsLong(elfHeader, "overall_align");
            Map<String, Result.Info> infos = new HashMap<>();
            for (Map.Entry<String, Class<?>> entry : members.entrySet()) {
                final String memberName = entry.getKey();
                final long memberSize = getSymbolValueAsLong(elfHeader, "sizeof_" + memberName);
                final long memberOffset = getSymbolValueAsLong(elfHeader, "offsetof_" + memberName);
                final Result.Info info = new Result.Info(memberSize, memberOffset);
                infos.put(memberName, info);
            }
            return new Result(overallSize, overallAlign, infos);
        }
    }

    private long getSymbolValueAsLong(final ElfHeader elfHeader, final String sym) {
        final ElfSymbolTableEntry symbol = elfHeader.findSymbol(sym);
        if (symbol == null) {
            throw new NoSuchElementException("Symbol \"" + sym + "\" not found in object file");
        }
        final long size = symbol.getValueSize();
        final int linkedSection = symbol.getLinkedSectionIndex();
        final ElfSectionHeaderEntry codeSection = elfHeader.getSectionHeaderTableEntry(linkedSection);
        if (codeSection.getType() == Elf.Section.Type.Std.NO_BITS) {
            // bss
            return 0;
        }
        if (size == 8) {
            return elfHeader.getBackingBuffer().getLong(codeSection.getOffset() + symbol.getValue());
        } else if (size == 4) {
            return elfHeader.getBackingBuffer().getIntUnsigned(codeSection.getOffset() + symbol.getValue());
        } else {
            throw new IllegalArgumentException("Unexpected size " + size);
        }
    }

    private UnaryOperator<StringBuilder> decl(String name, UnaryOperator<StringBuilder> value) {
        return decl(typeof(value), name, value);
    }

    private UnaryOperator<StringBuilder> decl(UnaryOperator<StringBuilder> type, String name,
            UnaryOperator<StringBuilder> value) {
        return sb -> value.apply(type.apply(sb).append(' ').append(name).append(" = ")).append(";\n");
    }

    private UnaryOperator<StringBuilder> plainType(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append(name));
    }

    private UnaryOperator<StringBuilder> memberOf(UnaryOperator<StringBuilder> type, String memberName) {
        return sb -> type.apply(sb.append("((")).append(" *) 0)->").append(memberName);
    }

    private UnaryOperator<StringBuilder> union(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("union ").append(name));
    }

    private UnaryOperator<StringBuilder> struct(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("struct ").append(name));
    }

    private UnaryOperator<StringBuilder> sizeof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("sizeof(")).append(')');
    }

    private UnaryOperator<StringBuilder> typeof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("typeof(")).append(')');
    }

    private UnaryOperator<StringBuilder> alignof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("_Alignof(")).append(')');
    }

    private UnaryOperator<StringBuilder> offsetof(UnaryOperator<StringBuilder> type, String memberName) {
        return sb -> type.apply(sb.append("offsetof(")).append(',').append(memberName).append(')');
    }

    public static class Result {
        private final long size;
        private final long align;
        private final Map<String, Info> infos;

        Result(final long size, final long align, final Map<String, Info> infos) {
            this.size = size;
            this.align = align;
            this.infos = infos;
        }

        public long getOverallSize() {
            return size;
        }

        public long getOverallAlign() {
            return align;
        }

        public long getMemberSize(String member) {
            return requireInfo(member).size;
        }

        public long getMemberOffset(String member) {
            return requireInfo(member).offset;
        }

        private Info requireInfo(final String member) {
            final Info info = infos.get(member);
            if (info == null) {
                throw new NoSuchElementException(member);
            }
            return info;
        }

        static final class Info {
            private final long size;
            private final long offset;

            Info(final long size, final long offset) {
                this.size = size;
                this.offset = offset;
            }
        }
    }

    public enum Qualifier {
        NONE,
        STRUCT,
        UNION,
        ;
    }

}
