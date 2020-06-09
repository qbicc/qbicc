package cc.quarkus.qcc.compiler.frontend.java;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import cc.quarkus.qcc.compiler.frontend.api.FrontEnd;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.spi.ClassFinder;
import cc.quarkus.qcc.type.universe.Universe;

/**
 *
 */
public class JavaFrontEnd implements FrontEnd {
    public String getName() {
        return "java";
    }

    public Universe compile() {
        final Context context = Context.requireCurrent();
        // todo: get from config
        JavaFrontEndConfig config = new JavaFrontEndConfig() {
            public List<Path> applicationClassPath() {
                return List.of(Paths.get(System.getProperty("qcc.compile.class-path")));
            }
        };
        final List<Path> paths = config.applicationClassPath();
        final Universe universe = new Universe(new ClassFinder() {
            public InputStream findClass(final String name) throws ClassNotFoundException, IOException {
                // TODO: this is really wrong; actually we want to find classes via invoking class loader
                // but it'll do to start with
                final String classFileName = name.replace('.', '/') + ".class";
                for (Path path : paths) {
                    try {
                        return Files.newInputStream(path.resolve(classFileName));
                    } catch (FileNotFoundException | NoSuchFileException ignored) {}
                }
                // now try our own JDK - something we're definitely NOT going to do "for real"
                final InputStream stream = getClass().getClassLoader().getResourceAsStream(classFileName);
                if (stream != null) {
                    return stream;
                }
                throw new ClassNotFoundException(name);
            }
        });
        Universe.setRootUniverse(universe);
        return universe;
    }
}
