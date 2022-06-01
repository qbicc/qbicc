package org.qbicc.machine.tool.emscripten;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class EmscriptenLinkerInvokerImpl extends AbstractEmscriptenInvoker implements EmscriptenLinkerInvoker {
    private final List<Path> libraryPaths = new ArrayList<>(4);
    private final List<String> libraries = new ArrayList<>(4);
    private final List<Path> objectFiles = new ArrayList<>(4);
    private Path outputPath = TMP.resolve("qbicc-output-image");
    private boolean isPie = false;

    EmscriptenLinkerInvokerImpl(final EmscriptenToolChainImpl tool) {
        super(tool);
    }

    public void addLibraryPath(final Path path) {
        libraryPaths.add(Assert.checkNotNullParam("path", path));
    }

    public int getLibraryPathCount() {
        return libraryPaths.size();
    }

    public Path getLibraryPath(final int index) throws IndexOutOfBoundsException {
        return libraryPaths.get(index);
    }

    public void addLibrary(final String name) {
        libraries.add(Assert.checkNotNullParam("name", name));
    }

    public int getLibraryCount() {
        return libraries.size();
    }

    public String getLibrary(final int index) throws IndexOutOfBoundsException {
        return libraries.get(index);
    }

    public void addObjectFile(final Path path) {
        objectFiles.add(Assert.checkNotNullParam("path", path));
    }

    public int getObjectFileCount() {
        return objectFiles.size();
    }

    public Path getObjectFile(final int index) throws IndexOutOfBoundsException {
        return objectFiles.get(index);
    }

    public void setOutputPath(final Path path) {
        outputPath = Assert.checkNotNullParam("path", path);
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setIsPie(boolean isPie) {
        this.isPie = isPie;
    }

    public boolean getIsPie() {
        return isPie;
    }

    void addArguments(final List<String> cmd) {
        if (isPie) {
            cmd.add("-pie");
        } else {
            cmd.add("-no-pie");
        }
        cmd.add("-pthread");

        for (Path libraryPath : libraryPaths) {
            cmd.add("-L" + libraryPath.toString());
        }
        for (String library : libraries) {
            cmd.add("-l" + library);
        }
        for (Path objectFile : objectFiles) {
            cmd.add(objectFile.toString());
        }
        cmd.add("-o");
        cmd.add(outputPath.toString());
    }
}
