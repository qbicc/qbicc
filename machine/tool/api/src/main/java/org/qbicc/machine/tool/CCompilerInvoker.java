package org.qbicc.machine.tool;

import java.nio.file.Path;

import org.qbicc.machine.tool.process.InputSource;

/**
 * An invoker for a C compiler.
 */
public interface CCompilerInvoker extends MessagingToolInvoker {
    /**
     * Add an include path.
     *
     * @param path the include path to add (must not be {@code null})
     */
    void addIncludePath(Path path);

    /**
     * Add all the given include paths.
     *
     * @param paths the include paths to add (must not be {@code null})
     */
    default void addIncludePaths(Iterable<Path> paths) {
        for (Path path : paths) {
            addIncludePath(path);
        }
    }

    /**
     * Get the number of include paths defined.
     *
     * @return the number of include paths
     */
    int getIncludePathCount();

    /**
     * Get the include path at the given index.
     *
     * @param index the index
     * @return the include path
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    Path getIncludePath(int index) throws IndexOutOfBoundsException;

    /**
     * Define a symbol to the C compiler to "1".
     *
     * @param name the symbol to define (must not be {@code null})
     */
    default void addDefinedSymbol(String name) {
        addDefinedSymbol(name, "1");
    }

    /**
     * Define a symbol to the C compiler.
     *
     * @param name the symbol to define (must not be {@code null})
     * @param value the value to give to the defined symbol (must not be {@code null})
     */
    void addDefinedSymbol(String name, String value);

    /**
     * Get the number of defined symbols.
     *
     * @return the number of defined symbols
     */
    int getDefinedSymbolCount();

    /**
     * Get the defined symbol at the given index.
     *
     * @param index the index
     * @return the defined symbol
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    String getDefinedSymbol(int index) throws IndexOutOfBoundsException;

    /**
     * Get the defined symbol value.
     *
     * @param index the index
     * @return the defined symbol value
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    String getDefinedSymbolValue(int index) throws IndexOutOfBoundsException;

    /**
     * Set the input source for the C program.
     *
     * @param source the input source (must not be {@code null})
     */
    void setSource(InputSource source);

    /**
     * Get the input source for the C program.
     *
     * @return the input source (not {@code null})
     */
    InputSource getSource();

    /**
     * Set the object file output path.
     *
     * @param path the output path (must not be {@code null})
     */
    void setOutputPath(Path path);

    SourceLanguage getSourceLanguage();

    void setSourceLanguage(SourceLanguage sourceLanguage);

    /**
     * Get the object file output path.
     *
     * @return the output path
     * @throws IllegalArgumentException if the output path has not been set
     */
    Path getOutputPath() throws IllegalArgumentException;

    enum SourceLanguage {
        C,
        ASM,
        ;
    }
}
