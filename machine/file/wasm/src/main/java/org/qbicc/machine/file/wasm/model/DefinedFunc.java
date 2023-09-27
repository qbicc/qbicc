package org.qbicc.machine.file.wasm.model;

import java.util.List;
import java.util.stream.IntStream;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionConsumer;
import org.qbicc.machine.file.wasm.FuncType;

/**
 * A function defined in its module.
 */
public record DefinedFunc(String name, FuncType type, List<Local> parameters, List<Local> locals, InsnSeq body) implements Func, Defined, BranchTarget {
    public DefinedFunc {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("parameters", parameters);
        Assert.checkNotNullParam("locals", locals);
        Assert.checkNotNullParam("body", body);
        parameters = List.copyOf(parameters);
        locals = List.copyOf(locals);
    }

    public DefinedFunc(String name, FuncType type) {
        this(name, type, List.of());
    }

    public DefinedFunc(String name, FuncType type, List<Local> locals) {
        this(name, type, computeParameters(type), locals, new InsnSeq());
    }

    public DefinedFunc(String name, FuncType type, List<Local> parameters, List<Local> locals) {
        this(name, type, parameters, locals, new InsnSeq());
    }

    public <E extends Exception> DefinedFunc(String name, FuncType type, List<Local> locals, InsnSeq body, ExceptionConsumer<DefinedFunc, E> builder) throws E {
        this(name, type, computeParameters(type), locals, body);
        builder.accept(this);
        body.end();
    }

    public <E extends Exception> DefinedFunc(String name, FuncType type, List<Local> parameters, List<Local> locals, InsnSeq body, ExceptionConsumer<DefinedFunc, E> builder) throws E {
        this(name, type, parameters, locals, body);
        builder.accept(this);
        body.end();
    }

    public <E extends Exception> DefinedFunc(String name, FuncType type, List<Local> locals, ExceptionConsumer<DefinedFunc, E> builder) throws E {
        this(name, type, computeParameters(type), locals, new InsnSeq());
        builder.accept(this);
        body.end();
    }

    public <E extends Exception> DefinedFunc(String name, FuncType type, List<Local> parameters, List<Local> locals, ExceptionConsumer<DefinedFunc, E> builder) throws E {
        this(name, type, parameters, locals, new InsnSeq());
        builder.accept(this);
        body.end();
    }

    private static List<Local> computeParameters(final FuncType type) {
        return IntStream.range(0, type.parameterTypes().size()).mapToObj(idx -> new Local("", type.parameterTypes().get(idx), idx)).toList();
    }
}
