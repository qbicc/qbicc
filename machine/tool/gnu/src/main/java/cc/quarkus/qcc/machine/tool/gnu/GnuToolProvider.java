package cc.quarkus.qcc.machine.tool.gnu;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.quarkus.qcc.machine.arch.Cpu;
import cc.quarkus.qcc.machine.arch.OS;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

/**
 * The provider for GNU tools.
 */
public class GnuToolProvider implements ToolProvider {
    public GnuToolProvider() {
    }

    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        final ArrayList<T> list = new ArrayList<>();
        if (type.isAssignableFrom(GccToolChainImpl.class)) {
            final String cpuSimpleName = platform.getCpu().getSimpleName();
            final String osName = platform.getOs().getName();
            final String abiName = platform.getAbi().getName();
            for (String name : List.of("gcc", cpuSimpleName + "-" + osName + "-" + abiName + "-gcc")) {
                tryGcc(type, platform, list, name);
            }
            return list;
        } else {
            return List.of();
        }
    }

    static final Pattern TARGET_PATTERN = Pattern.compile("^Target:\\s+(x86_64|arm|i[3-6]86|aarch64|powerpc64)(?:-(redhat|apple|ibm|unknown))?-(linux|darwin)(?:-(gnu|gnueabi))?");
    static final Pattern VERSION_PATTERN = Pattern.compile("^gcc version (\\S+)");

    private <T extends Tool> void tryGcc(final Class<T> type, final Platform platform, final ArrayList<T> list, final String name) {
        final Path path = ToolUtil.findExecutable(name);
        if (path != null && Files.isExecutable(path)) {
            class Result {
                String version;
                boolean m32;
                boolean match;
            }
            Result res = new Result();
            OutputDestination dest = OutputDestination.of(r -> {
                try (BufferedReader br = new BufferedReader(r)) {
                    String line;
                    Matcher matcher;
                    while ((line = br.readLine()) != null) {
                        matcher = TARGET_PATTERN.matcher(line);
                        if (matcher.find()) {
                            String cpuStr = matcher.group(1);
                            // String vendorStr = matcher.group(2);
                            String osStr = matcher.group(3);
                            String abiStr = matcher.group(4);
                            Cpu cpu = Cpu.forName(cpuStr);
                            boolean m32 = platform.getCpu().equals(Cpu.X86) && cpu.equals(Cpu.X86_64);
                            boolean cpuMatch = cpu.equals(platform.getCpu()) || m32;
                            OS os = OS.forName(osStr);
                            boolean osMatch = os.equals(platform.getOs());
                            // todo: ABI match might be a little trickier
                            res.match = cpuMatch && osMatch;
                            res.m32 = m32;
                        } else {
                            matcher = VERSION_PATTERN.matcher(line);
                            if (matcher.find()) {
                                res.version = matcher.group(1);
                            }
                        }
                    }
                }
            }, StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder(path.toString(), "-###");
            try {
                InputSource.empty().transferTo(OutputDestination.of(pb, dest, OutputDestination.discarding()));
            } catch (IOException e) {
                // ignore invalid compiler
                return;
            }
            if (res.match) {
                final GccToolChainImpl gcc = new GccToolChainImpl(path, platform, res.version, res.m32);
                list.add(type.cast(gcc));
            }
        }
    }
}
