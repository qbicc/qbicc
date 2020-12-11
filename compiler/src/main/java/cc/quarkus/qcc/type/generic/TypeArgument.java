package cc.quarkus.qcc.type.generic;

import static cc.quarkus.qcc.type.generic.Signature.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.type.definition.ClassContext;

/**
 * Any kind of type argument.
 */
public abstract class TypeArgument {
    private final int hashCode;

    TypeArgument(final int hashCode) {
        this.hashCode = hashCode;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(final Object obj) {
        return obj instanceof TypeArgument && equals((TypeArgument) obj);
    }

    public boolean equals(final TypeArgument other) {
        return this == other || other != null && hashCode == other.hashCode;
    }

    public final String toString() {
        return toString(new StringBuilder()).toString();
    }

    public abstract StringBuilder toString(StringBuilder target);

    static TypeArgument parse(ClassContext classContext, ByteBuffer buf) {
        if (peek(buf) == '*') {
            return AnyTypeArgument.parse(buf);
        } else {
            return BoundTypeArgument.parse(classContext, buf);
        }
    }

    static List<TypeArgument> parseList(ClassContext classContext, ByteBuffer buf) {
        int i = next(buf);
        if (i != '<') {
            throw parseError();
        }
        i = peek(buf);
        if (i != '>') {
            TypeArgument a = parse(classContext, buf);
            i = peek(buf);
            if (i != '>') {
                TypeArgument b = parse(classContext, buf);
                i = peek(buf);
                if (i != '>') {
                    TypeArgument c = parse(classContext, buf);
                    i = peek(buf);
                    if (i != '>') {
                        TypeArgument d = parse(classContext, buf);
                        i = peek(buf);
                        if (i != '>') {
                            List<TypeArgument> list = new ArrayList<>();
                            Collections.addAll(list, a, b, c, d);
                            do {
                                list.add(parse(classContext, buf));
                                i = peek(buf);
                            } while (i != '>');
                            buf.get(); // consume '>'
                            return List.copyOf(list);
                        } else {
                            buf.get(); // consume '>'
                            return List.of(a, b, c, d);
                        }
                    } else {
                        buf.get(); // consume '>'
                        return List.of(a, b, c);
                    }
                } else {
                    buf.get(); // consume '>'
                    return List.of(a, b);
                }
            } else {
                buf.get(); // consume '>'
                return List.of(a);
            }
        } else {
            buf.get(); // consume '>'
            return List.of();
        }
    }
}
