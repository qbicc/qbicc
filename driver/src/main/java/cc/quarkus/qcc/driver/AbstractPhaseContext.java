package cc.quarkus.qcc.driver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.element.BasicElement;

/**
 *
 */
abstract class AbstractPhaseContext implements CompilationContext {
    final BaseContext baseContext;
    final List<BasicBlockBuilder.Factory> factories;

    AbstractPhaseContext(final BaseContext baseContext, final List<BasicBlockBuilder.Factory> factories) {
        this.baseContext = baseContext;
        this.factories = factories;
    }

    public <T> T getAttachment(final AttachmentKey<T> key) {
        return baseContext.getAttachment(key);
    }

    public <T> T getAttachmentOrDefault(final AttachmentKey<T> key, final T defVal) {
        return baseContext.getAttachmentOrDefault(key, defVal);
    }

    public <T> T putAttachment(final AttachmentKey<T> key, final T value) {
        return baseContext.putAttachment(key, value);
    }

    public <T> T putAttachmentIfAbsent(final AttachmentKey<T> key, final T value) {
        return baseContext.putAttachmentIfAbsent(key, value);
    }

    public <T> T removeAttachment(final AttachmentKey<T> key) {
        return baseContext.removeAttachment(key);
    }

    public <T> boolean removeAttachment(final AttachmentKey<T> key, final T expect) {
        return baseContext.removeAttachment(key, expect);
    }

    public <T> T replaceAttachment(final AttachmentKey<T> key, final T update) {
        return baseContext.replaceAttachment(key, update);
    }

    public <T> boolean replaceAttachment(final AttachmentKey<T> key, final T expect, final T update) {
        return baseContext.replaceAttachment(key, expect, update);
    }

    public <T> T computeAttachmentIfAbsent(final AttachmentKey<T> key, final Supplier<T> function) {
        return baseContext.computeAttachmentIfAbsent(key, function);
    }

    public <T> T computeAttachmentIfPresent(final AttachmentKey<T> key, final Function<T, T> function) {
        return baseContext.computeAttachmentIfPresent(key, function);
    }

    public <T> T computeAttachment(final AttachmentKey<T> key, final Function<T, T> function) {
        return baseContext.computeAttachment(key, function);
    }

    public int errors() {
        return baseContext.errors();
    }

    public int warnings() {
        return baseContext.warnings();
    }

    public Diagnostic msg(final Diagnostic parent, final Location loc, final Diagnostic.Level level, final String fmt, final Object... args) {
        return baseContext.msg(parent, loc, level, fmt, args);
    }

    public Diagnostic msg(final Diagnostic parent, final BasicElement element, final Node node, final Diagnostic.Level level, final String fmt, final Object... args) {
        return baseContext.msg(parent, element, node, level, fmt, args);
    }

    public Iterable<Diagnostic> getDiagnostics() {
        return baseContext.getDiagnostics();
    }

    public TypeSystem getTypeSystem() {
        return baseContext.getTypeSystem();
    }

    public LiteralFactory getLiteralFactory() {
        return baseContext.getLiteralFactory();
    }

    public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        return baseContext.deduplicate(buffer, offset, length);
    }

    public String deduplicate(final String original) {
        return baseContext.deduplicate(original);
    }

    public BasicBlockBuilder newBasicBlockBuilder() {
        BasicBlockBuilder result = null;
        for (BasicBlockBuilder.Factory factory : factories) {
            result = factory.construct(this, result);
        }
        return result;
    }
}
