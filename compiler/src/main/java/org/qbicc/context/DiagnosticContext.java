package org.qbicc.context;

import java.util.function.Function;
import java.util.function.Supplier;

import org.qbicc.graph.Node;
import org.qbicc.type.definition.element.Element;

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

    Diagnostic msg(Diagnostic parent, Element element, Node node, Diagnostic.Level level, String fmt, Object... args);

    default Diagnostic error(Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Element element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Diagnostic parent, Element element, String fmt, Object... args) {
        return msg(parent, element, null, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Element element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Diagnostic parent, Location location, String fmt, Object... args) {
        return msg(parent, location, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Diagnostic parent, String fmt, Object... args) {
        return msg(parent, null, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.ERROR, fmt, args);
    }

    default Diagnostic error(Throwable t, String fmt, Object... args) {
        return error(t, null, null, null, fmt, args);
    }

    default Diagnostic error(Throwable t, Element element, String fmt, Object... args) {
        return error(t, null, element, null, fmt, args);
    }

    default Diagnostic error(Throwable t, Element element, Node node, String fmt, Object... args) {
        return error(t, null, element, node, fmt, args);
    }

    default Diagnostic error(Throwable t, Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        if (t == null) {
            return error(parent, element, node, fmt, args);
        }
        Diagnostic outer = error(parent, element, node, fmt, args);
        note(outer, Location.fromStackTrace(t), "This is the location of the exception");
        return outer;
    }

    default Diagnostic warning(Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Element element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Diagnostic parent, Element element, String fmt, Object... args) {
        return msg(parent, element, null, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Element element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Diagnostic parent, Location location, String fmt, Object... args) {
        return msg(parent, location, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Diagnostic parent, String fmt, Object... args) {
        return msg(parent, null, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.WARNING, fmt, args);
    }

    default Diagnostic warning(Throwable t, String fmt, Object... args) {
        return warning(t, null, null, null, fmt, args);
    }

    default Diagnostic warning(Throwable t, Element element, String fmt, Object... args) {
        return warning(t, null, element, null, fmt, args);
    }

    default Diagnostic warning(Throwable t, Element element, Node node, String fmt, Object... args) {
        return warning(t, null, element, node, fmt, args);
    }

    default Diagnostic warning(Throwable t, Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        if (t == null) {
            return warning(parent, element, node, fmt, args);
        }
        Diagnostic outer = warning(parent, element, node, fmt, args);
        note(outer, Location.fromStackTrace(t), "This is the location of the exception");
        return outer;
    }

    default Diagnostic note(Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Element element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Diagnostic parent, Element element, String fmt, Object... args) {
        return msg(parent, element, null, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Element element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Diagnostic parent, Location location, String fmt, Object... args) {
        return msg(parent, location, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(Diagnostic parent, String fmt, Object... args) {
        return msg(parent, null, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic note(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.NOTE, fmt, args);
    }

    default Diagnostic info(Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Element element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Diagnostic parent, Element element, String fmt, Object... args) {
        return msg(parent, element, null, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Element element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Diagnostic parent, Location location, String fmt, Object... args) {
        return msg(parent, location, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(Diagnostic parent, String fmt, Object... args) {
        return msg(parent, null, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic info(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.INFO, fmt, args);
    }

    default Diagnostic debug(Diagnostic parent, Element element, Node node, String fmt, Object... args) {
        return msg(parent, element, node, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Element element, Node node, String fmt, Object... args) {
        return msg(null, element, node, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Diagnostic parent, Element element, String fmt, Object... args) {
        return msg(parent, element, null, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Element element, String fmt, Object... args) {
        return msg(null, element, null, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Diagnostic parent, Location location, String fmt, Object... args) {
        return msg(parent, location, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Location location, String fmt, Object... args) {
        return msg(null, location, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(Diagnostic parent, String fmt, Object... args) {
        return msg(parent, null, Diagnostic.Level.DEBUG, fmt, args);
    }

    default Diagnostic debug(String fmt, Object... args) {
        return msg(null, null, null, Diagnostic.Level.DEBUG, fmt, args);
    }

    Iterable<Diagnostic> getDiagnostics();
}
