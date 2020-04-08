package cc.quarkus.qcc.machine.tool.gnu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.diagnostic.DiagnosticContext;
import cc.quarkus.qcc.machine.tool.CompilationResult;
import cc.quarkus.qcc.machine.tool.CompilerInvocationBuilder;
import cc.quarkus.qcc.machine.tool.InputSource;

/**
 *
 */
public class GccInvocationBuilder extends CompilerInvocationBuilder<GccInvocationBuilder.Param> {

    GccInvocationBuilder(final GccCompiler gcc) {
        super(gcc);
    }

    protected ProcessBuilder createProcessBuilder(Param param) {
        final ProcessBuilder pb = super.createProcessBuilder(param);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        //noinspection SpellCheckingInspection
        pb.command().addAll(List.of("-std=gnu11", "-finput-charset=UTF-8", "-pipe", "-c", "-x", "c", "-o", param.outputFile.toString(), "-"));
        return pb;
    }

    protected Param createCollectorParam() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("mandrel-");
        final Path probeObjPath = tempDirectory.resolve("probe.o");
        probeObjPath.toFile().deleteOnExit();
        return new Param(this, probeObjPath);
    }

    protected int waitForProcessUninterruptibly(final Process p) {
        return super.waitForProcessUninterruptibly(p);
    }

    protected void collectError(final Param param, final InputStream stream) throws Exception {
        final InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
        final BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            DiagnosticContext.error(null, "%s", line);
        }
    }

    public GccInvocationBuilder setInputSource(final InputSource inputSource) {
        super.setInputSource(inputSource);
        return this;
    }

    protected CompilationResult produceResult(final Param param, final Process process) throws Exception {
        final int res = waitForProcessUninterruptibly(process);
        if (res != 0) {
            DiagnosticContext.error(null, "Process returned exit code %d", Integer.valueOf(res));
            return null;
        }
        if (Files.exists(param.outputFile)) {
            return new GccCompilationResult(param.outputFile);
        } else {
            return null;
        }
    }

    static final class Param {
        final GccInvocationBuilder outer;
        final Path outputFile;

        Param(final GccInvocationBuilder outer, final Path outputFile) {
            this.outer = outer;
            this.outputFile = outputFile;
        }
    }
}
