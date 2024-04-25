package org.qbicc.test;

import static io.smallrye.common.constraint.Assert.unreachableCode;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.BaseDiagnosticContext;
import org.qbicc.driver.Driver;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.type.TypeSystem;

/**
 * A class which is usable as a base class for simple test cases which use the compiler and need a compiler context.
 */
public abstract class AbstractCompilerTestCase {
    public static CompilationContext ctxt;
    public static ClassContext bootClassContext;
    public static TypeSystem ts;
    public static LiteralFactory lf;
    private static Driver driver;

    @BeforeAll
    protected static void setUp() {
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(new BaseDiagnosticContext());
        builder.setOutputDirectory(Path.of(System.getProperty("user.dir", "."), "target", "test-fwk"));
        final Platform platform = getPlatform();
        builder.setTargetPlatform(platform);
        Optional<ObjectFileProvider> ofp = ObjectFileProvider.findProvider(platform.objectType(), AbstractCompilerTestCase.class.getClassLoader());
        if (ofp.isEmpty()) {
            fail("No object file provider found for " + platform);
            throw unreachableCode();
        }
        builder.setObjectFileProvider(ofp.get());
        Iterator<CToolChain> toolChains = CToolChain.findAllCToolChains(platform, t -> true, AbstractCompilerTestCase.class.getClassLoader()).iterator();
        if (! toolChains.hasNext()) {
            fail("No tool chains found for " + platform);
            throw unreachableCode();
        }
        builder.setToolChain(toolChains.next());
        Iterator<LlvmToolChain> llvmTools = LlvmToolChain.findAllLlvmToolChains(platform, t -> true, AbstractCompilerTestCase.class.getClassLoader()).iterator();
        LlvmToolChain llvmToolChain = null;
        List<String> tried = new ArrayList<>();
        while (llvmTools.hasNext()) {
            llvmToolChain = llvmTools.next();
            if (llvmToolChain.compareVersionTo("16") >= 0) {
                // found it
                break;
            }
            tried.add(llvmToolChain.getVersion());
            llvmToolChain = null;
        }
        if (llvmToolChain == null) {
            fail("No LLVM tool chain found (found versions: " + tried + ")");
            throw unreachableCode();
        } else {
            builder.setLlvmToolChain(llvmToolChain);
        }
        final TypeSystem.Builder tsBuilder = TypeSystem.builder();
        final TypeSystem ts = tsBuilder.build();
        builder.setTypeSystem(ts);
        AbstractCompilerTestCase.ts = ts;
        builder.setVmFactory(CompilationContext::getVm);

        final Driver driver = builder.build();
        ctxt = driver.getCompilationContext();
        bootClassContext = ctxt.getBootstrapClassContext();
        lf = ctxt.getLiteralFactory();
        AbstractCompilerTestCase.driver = driver;
    }

    @AfterAll
    protected static void cleanUp() {
        // avoid leaking things
        driver.close();
    }

    static Platform getPlatform() {
        return Platform.HOST_PLATFORM;
    }
}
