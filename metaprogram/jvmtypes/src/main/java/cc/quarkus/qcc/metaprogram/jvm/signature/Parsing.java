package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 *
 */
final class Parsing {
    private static final ClassTypeSignature.TypeArgument[] NO_ARGUMENTS = new ClassTypeSignature.TypeArgument[0];

    // this sucks but Java makes it really hard to do recursive-descent parsing on a string
    private static final AttachmentKey<ParsingContext> PC_KEY = new AttachmentKey<>();

    private Parsing() {}

    static TypeSignature parseTypeSignature(String signature) {
        final ParsingCache parsingCache = ParsingCache.get();
        final ParsingContext pc = Context.requireCurrent().computeAttachmentIfAbsent(PC_KEY, ParsingContext::new);
        int old = pc.pos;
        try {
            return parseTypeSignature(parsingCache, signature, pc);
        } finally {
            pc.pos = old;
        }
    }

    static TypeSignature parseTypeSignature(ParsingCache cache, String signature, ParsingContext pc) {
        char ch = signature.charAt(pc.pos);
        // reference or base type
        final BaseTypeSignature bts = BaseTypeSignature.forCharacter(ch);
        if (bts != null) {
            return bts;
        } else {
            return parseReferenceTypeSignature(cache, signature, pc);
        }
    }

    static ReferenceTypeSignature parseReferenceTypeSignature(ParsingCache cache, String signature, ParsingContext pc) {
        char ch = signature.charAt(pc.pos);
        String name;
        // it's a reference of some sort
        if (ch == 'T') {
            // type variable
            int start = pc.pos + 1;
            do {
                pc.pos ++;
                if (pc.pos == signature.length()) {
                    throw new IllegalArgumentException("Unterminated type variable");
                }
                ch = signature.charAt(pc.pos);
                if (ch == '/' || ch == '.' || ch == '<' || ch == '>' || ch == '*' || ch == '+' || ch == '-') {
                    throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
                }
            } while (ch != ';');
            name = cache.getCachedName(signature, start, pc.pos++);
            return cache.getTypeVariableNamed(name);
        } else if (ch == 'L') {
            // class possibly in a package
            pc.pos ++;
            return parseClassTypeSignature(cache, signature, pc, null, null);
        } else if (ch == '[') {
            // array
            pc.pos ++;
            return cache.getArrayOf(parseTypeSignature(cache, signature, pc));
        } else {
            throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
        }
    }

    static ClassTypeSignature parseClassTypeSignature(ParsingCache cache, String signature, ParsingContext pc, PackageName packageName, ClassTypeSignature enclosing) {
        assert packageName == null || enclosing == null;
        char ch;
        int start = pc.pos;
        String name;
        for (;;) {
            if (pc.pos == signature.length()) {
                throw new IllegalArgumentException("Unterminated class type name");
            }
            ch = signature.charAt(pc.pos);
            if (ch == '/') {
                if (enclosing != null) {
                    throw new IllegalArgumentException("Package name in enclosed class type signature");
                }
                name = cache.getCachedName(signature, start, pc.pos++);
                return parseClassTypeSignature(cache, signature, pc, cache.getPackageNamed(packageName, name), null);
            } else if (ch == '.') {
                name = cache.getCachedName(signature, start, pc.pos++);
                return parseClassTypeSignature(cache, signature, pc, null, cache.getTypeSignature(packageName, enclosing, name));
            } else if (ch == ';') {
                name = cache.getCachedName(signature, start, pc.pos++);
                return cache.getTypeSignature(packageName, enclosing, name);
            } else if (ch == '<') {
                // arguments!
                name = cache.getCachedName(signature, start, pc.pos++);
                final ClassTypeSignature thisWithArgs = parseClassTypeSignatureArgs(cache, signature, pc, cache.getTypeSignature(packageName, enclosing, name));
                ch = signature.charAt(pc.pos);
                if (ch == '.') {
                    pc.pos++;
                    return parseClassTypeSignature(cache, signature, pc, null, thisWithArgs);
                } else if (ch == ';') {
                    pc.pos++;
                    return thisWithArgs;
                } else {
                    throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
                }
            } else {
                // identifier part
                pc.pos++;
            }
        }
    }

    static ClassTypeSignature parseClassTypeSignatureArgs(ParsingCache cache, String signature, ParsingContext pc, ClassTypeSignature delegate) {
        char ch;
        int start = pc.pos;
        if (pc.pos == signature.length()) {
            throw new IllegalArgumentException("Unterminated type signature");
        }
        ch = signature.charAt(pc.pos);
        if (ch == '>') {
            if (delegate.getTypeArgumentCount() == 0) {
                throw new IllegalArgumentException("Empty type arguments list");
            } else {
                pc.pos++;
                return delegate;
            }
        } else if (ch == '*') {
            pc.pos++;
            return parseClassTypeSignatureArgs(cache, signature, pc, cache.getTypeSignature(delegate, ClassTypeSignature.AnyTypeArgument.INSTANCE));
        } else {
            ClassTypeSignature.Variance v;
            if (ch == '+') {
                pc.pos++;
                v = ClassTypeSignature.Variance.COVARIANT;
            } else if (ch == '-') {
                pc.pos++;
                v = ClassTypeSignature.Variance.CONTRAVARIANT;
            } else {
                v = ClassTypeSignature.Variance.INVARIANT;
            }
            final ReferenceTypeSignature nested = parseReferenceTypeSignature(cache, signature, pc);
            return parseClassTypeSignatureArgs(cache, signature, pc, cache.getTypeSignature(delegate, cache.getBoundTypeArgument(v, nested)));
        }
    }

    static final class ParsingContext {
        int pos;
    }
}
