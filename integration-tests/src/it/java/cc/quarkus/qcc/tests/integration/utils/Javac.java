package cc.quarkus.qcc.tests.integration.utils;

import org.jboss.logging.Logger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.nio.file.Path;
import java.util.List;

public class Javac {

    public static boolean compile(Path outputPath, Path source, Logger logger) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjects(source);
        List<String> options = List.of(
            "--release", TestConstants.MAVEN_COMPILER_RELEASE,
            "-d", outputPath.toString(),
            "-cp", TestConstants.QCC_RUNTIME_API_JAR);

        JavaCompiler.CompilationTask task =
            javac.getTask(
                null,
                fileManager,
                new JavacDiagnosticListener(logger), // append to list all logs
                options,
                null,
                javaFileObjects);

        return task.call();
    }
}
