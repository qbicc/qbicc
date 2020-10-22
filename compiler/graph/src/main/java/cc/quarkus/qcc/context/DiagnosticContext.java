package cc.quarkus.qcc.context;

import java.util.function.Function;
import java.util.function.Supplier;

import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.type.definition.element.BasicElement;

/**
 *
 */
public interface DiagnosticContext {
    <T> T getAttachment(AttachmentKey<T> key);

    <T> T getAttachmentOrDefault(AttachmentKey<T> key, T defVal);

    <T> T putAttachment(AttachmentKey<T> key, T value);

    <T> T putAttachmentIfAbsent(AttachmentKey<T> key, T value);

    <T> T removeAttachment(AttachmentKey<T> key);

    <T> boolean removeAttachment(AttachmentKey<T> key, T expect);

    <T> T replaceAttachment(AttachmentKey<T> key, T update);

    <T> boolean replaceAttachment(AttachmentKey<T> key, T expect, T update);

    <T> T computeAttachmentIfAbsent(AttachmentKey<T> key, Supplier<T> function);

    <T> T computeAttachmentIfPresent(AttachmentKey<T> key, Function<T, T> function);

    <T> T computeAttachment(AttachmentKey<T> key, Function<T, T> function);

    int errors();

    int warnings();

    Diagnostic msg(Diagnostic parent, Location location, Diagnostic.Level level, String fmt, Object... args);

    Diagnostic msg(Diagnostic parent, BasicElement element, Node node, Diagnostic.Level level, String fmt, Object... args);

    default Diagnostic error(Diagnostic parent, BasicElement element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(BasicElement element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(BasicElement element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic warning(Diagnostic parent, BasicElement element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(BasicElement element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(BasicElement element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic note(Diagnostic parent, BasicElement element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(BasicElement element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(BasicElement element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic info(Diagnostic parent, BasicElement element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(BasicElement element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(BasicElement element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic debug(Diagnostic parent, BasicElement element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(BasicElement element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(BasicElement element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.DEBUG, fmt, args);
    }

    Iterable<Diagnostic> getDiagnostics();
}
