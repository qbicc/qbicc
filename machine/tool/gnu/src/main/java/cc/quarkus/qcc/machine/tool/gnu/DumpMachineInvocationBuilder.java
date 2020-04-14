package cc.quarkus.qcc.machine.tool.gnu;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.machine.arch.ABI;
import cc.quarkus.qcc.machine.arch.Cpu;
import cc.quarkus.qcc.machine.arch.OS;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.InvocationBuilder;
import cc.quarkus.qcc.machine.tool.Tool;

/**
 *
 */
final class DumpMachineInvocationBuilder extends InvocationBuilder<DumpMachineInvocationBuilder.Param, Platform> {
    private static final Pattern PROBE_PATTERN = generateProbePattern();

    DumpMachineInvocationBuilder(final Tool tool) {
        super(tool);
    }

    protected Param createCollectorParam() throws Exception {
        return new Param(this);
    }

    protected void collectOutput(final Param param, final InputStream stream) throws Exception {
        final byte[] bytes = stream.readAllBytes();
        param.platformString = new String(bytes, StandardCharsets.UTF_8).trim();
        param.latch.countDown();
    }

    protected void collectError(final Param param, final InputStream stream) throws Exception {
        final InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
        final BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            Context.error(null, "%s", line);
        }
    }

    protected ProcessBuilder createProcessBuilder(final Param param) {
        final ProcessBuilder pb = super.createProcessBuilder(param);
        pb.command().addAll(List.of("-dumpmachine"));
        return pb;
    }

    protected Platform produceResult(final Param param, final Process process) throws Exception {
        process.waitFor();
        param.latch.await();
        final Matcher matcher = PROBE_PATTERN.matcher(param.platformString);
        if (! matcher.matches()) {
            return new Platform(Cpu.UNKNOWN, OS.UNKNOWN, ABI.UNKNOWN);
        } else {
            final Cpu cpu = Cpu.forName(matcher.group(1));
            final OS os = OS.forName(matcher.group(2));
            final String abiName = matcher.group(3);
            final ABI abi = abiName == null ? os.getDefaultAbi(cpu) : ABI.forName(abiName);
            return new Platform(cpu, os, abi);
        }
    }

    private static Pattern generateProbePattern() {
        // GCC syntax is cpu[-vendor]-os[-abi], annoyingly
        StringBuilder pb = new StringBuilder();
        // put CPU name in group 1
        setToPatterns(pb, Cpu.getNames());
        // ignore vendor, if present
        pb.append("(?:-[^-]+)?");
        // next is OS in group 2
        setToPatterns(pb, OS.getNames());
        // finally optional ABI in group 3
        pb.append("(?:-");
        setToPatterns(pb, ABI.getNames());
        pb.append(")?"); // make it optional
        return Pattern.compile(pb.toString());
    }


    private static void setToPatterns(StringBuilder b, Set<String> set) {
        final Iterator<String> iter = set.iterator();
        if (iter.hasNext()) {
            b.append('(');
            b.append(Pattern.quote(iter.next()));
            while (iter.hasNext()) {
                b.append('|');
                b.append(Pattern.quote(iter.next()));
            }
            b.append(')');
        }
    }

    static final class Param {
        final DumpMachineInvocationBuilder outer;
        volatile String platformString;
        final CountDownLatch latch = new CountDownLatch(1);

        Param(final DumpMachineInvocationBuilder outer) {
            this.outer = outer;
        }
    }
}
