package cc.quarkus.qcc.machine.probe;

import static cc.quarkus.qcc.machine.probe.ProbeUtil.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.CompilationFailureException;
import cc.quarkus.qcc.machine.tool.CompilerInvokerBuilder;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import io.smallrye.common.constraint.Assert;

/**
 * A probe to determine whether a constant is defined (using {@code #define}) and optionally what its actual value is
 * in terms of a given type.
 */
public class CConstantProbe {
    private final String name;
    private final Qualifier qualifier;
    private final String typeName;
    private final int size;
    private final List<String> preproc = new ArrayList<>();

    public CConstantProbe(final String name, final Qualifier qualifier, final String typeName, final int size) {
        this.name = Assert.checkNotNullParam("name", name);
        this.qualifier = Assert.checkNotNullParam("qualifier", qualifier);
        this.typeName = Assert.checkNotNullParam("typeName", typeName);
        this.size = size;
    }

    public CConstantProbe(final String name) {
        this.name = Assert.checkNotNullParam("name", name);
        qualifier = Qualifier.NONE;
        typeName = null;
        size = 0;
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
        b.append("#include <stddef.h>\n");
        for (String str : preproc) {
            b.append(str).append('\n');
        }
        // guard with sizeof(size_t) in a (hopefully) 64-bit holder
        final UnaryOperator<StringBuilder> _Bool = literal("_Bool");
        decl(_Bool, "is_defined", defined(name)).apply(b);
        b.append('\n');
        if (typeName != null) {
            UnaryOperator<StringBuilder> type;
            if (qualifier == Qualifier.STRUCT) {
                type = struct(typeName, UnaryOperator.identity());
            } else if (qualifier == Qualifier.UNION) {
                type = union(typeName, UnaryOperator.identity());
            } else {
                assert qualifier == Qualifier.NONE;
                type = literal(typeName);
            }
            decl(type, "defined_value", definedValue(name, type)).apply(b);
        }
        b.append('\n');
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final Level level, final String file, final int line, final int column, final String message) {
                System.out.println(level + ": " + file + ":" + line + " -> " + message);
            }
        });
        final Path path = Files.createTempFile("qcc-probe-", "." + objectFileProvider.getObjectType().objectSuffix());
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
            boolean defined = objectFile.getSymbolValueAsByte("is_defined") != 0;
            byte[] value;
            if (typeName != null) {
                value = objectFile.getSymbolAsBytes("defined_value", size);
            } else {
                value = null;
            }
            return new Result(defined, value);
        }
    }

    public static class Result {
        private final boolean defined;
        private final byte[] value;

        Result(final boolean defined, final byte[] value) {
            this.defined = defined;
            this.value = value;
        }

        public boolean isDefined() {
            return defined;
        }

        public boolean hasValue() {
            return value != null;
        }

        public byte[] getValue() {
            return Assert.checkNotNullParam("value", value);
        }
    }
}
