package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;

/**
 * An invoker for a linker.
 */
public interface LinkerInvoker extends MessagingToolInvoker {
    void addLibraryPath(Path path);

    default void addLibraryPaths(Iterable<Path> paths) {
        for (Path path : paths) {
            addLibraryPath(path);
        }
    }

    int getLibraryPathCount();

    Path getLibraryPath(int index) throws IndexOutOfBoundsException;

    void addLibrary(String name);

    default void addLibraries(Iterable<String> libraries) {
        for (String library : libraries) {
            addLibrary(library);
        }
    }

    int getLibraryCount();

    String getLibrary(int index) throws IndexOutOfBoundsException;

    void addObjectFile(Path path);

    default void addObjectFiles(Iterable<Path> paths) {
        for (Path path : paths) {
            addObjectFile(path);
        }
    }

    int getObjectFileCount();

    Path getObjectFile(int index) throws IndexOutOfBoundsException;

    void setOutputPath(Path path);

    Path getOutputPath();
}
