package cc.quarkus.qcc.machine.probe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.CompilationFailureException;
import cc.quarkus.qcc.machine.tool.CompilerInvokerBuilder;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

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

    public Result runProbe(CCompiler compiler, ObjectFileProvider objectFileProvider) throws IOException {
        final CompilerInvokerBuilder ib = compiler.invocationBuilder();
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
        // guard with sizeof(size_t) in a (hopefully) 64-bit holder
        decl(plainType("unsigned long long", UnaryOperator.identity()), "size_t_size", sizeof(plainType("size_t", UnaryOperator.identity()))).apply(b);
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
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final Level level, final String file, final int line, final int column, final String message) {
                System.out.println(level + ": " + file + ":" + line + " -> " + message);
            }
        });
        final Path path = Files.createTempFile("qcc-probe-", objectFileProvider.getObjectType().objectSuffix());
        ib.setOutputPath(path);
        OutputDestination od = ib.build();
        try {
            InputSource.from(b).transferTo(od);
        } catch (CompilationFailureException e) {
            // no result
            return null;
        }
        // read back the symbol info
        try (ObjectFile objectFile = objectFileProvider.openObjectFile(path)) {
            long size_t_size = objectFile.getSymbolValueAsLong("size_t_size");
            boolean isLong = size_t_size == 8;
            long overallSize = isLong ? objectFile.getSymbolValueAsLong("overall_size") : objectFile.getSymbolValueAsInt("overall_size");
            long overallAlign = isLong ? objectFile.getSymbolValueAsLong("overall_align") : objectFile.getSymbolValueAsInt("overall_align");
            Map<String, Result.Info> infos = new HashMap<>();
            for (Map.Entry<String, Class<?>> entry : members.entrySet()) {
                final String memberName = entry.getKey();
                final long memberSize = isLong ? objectFile.getSymbolValueAsLong("sizeof_" + memberName) : objectFile.getSymbolValueAsInt("sizeof_" + memberName);
                final long memberOffset = isLong ? objectFile.getSymbolValueAsLong("offsetof_" + memberName) : objectFile.getSymbolValueAsInt("offsetof_" + memberName);
                final Result.Info info = new Result.Info(memberSize, memberOffset);
                infos.put(memberName, info);
            }
            return new Result(overallSize, overallAlign, infos);
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
