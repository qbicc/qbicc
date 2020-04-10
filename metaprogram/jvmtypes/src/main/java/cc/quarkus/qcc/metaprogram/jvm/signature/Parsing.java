package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 *
 */
final class Parsing {
    private static final TypeArgument[] NO_ARGUMENTS = new TypeArgument[0];
    private static final TypeParameter[] NO_PARAMS = new TypeParameter[0];

    // this sucks but Java makes it really hard to do recursive-descent parsing on a string
    private static final ThreadLocal<ParsingContext> PC_TL = ThreadLocal.withInitial(ParsingContext::new);

    private Parsing() {}

    static TypeSignature parseTypeSignature(String signature) {
        final ParsingCache parsingCache = ParsingCache.get();
        final ParsingContext pc = PC_TL.get();
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
            return parseSimpleClassTypeSignature(cache, signature, pc, null, null);
        } else if (ch == '[') {
            // array
            pc.pos ++;
            return cache.getArrayOf(parseTypeSignature(cache, signature, pc));
        } else {
            throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
        }
    }

    static ClassTypeSignature parseSimpleClassTypeSignature(ParsingCache cache, String signature, ParsingContext pc, PackageName packageName, ClassTypeSignature enclosing) {
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
                return parseSimpleClassTypeSignature(cache, signature, pc, cache.getPackageNamed(packageName, name), null);
            } else if (ch == '.') {
                name = cache.getCachedName(signature, start, pc.pos++);
                return parseSimpleClassTypeSignature(cache, signature, pc, null, cache.getTypeSignature(packageName, enclosing, name));
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
                    return parseSimpleClassTypeSignature(cache, signature, pc, null, thisWithArgs);
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
            return parseClassTypeSignatureArgs(cache, signature, pc, cache.getTypeSignature(delegate, AnyTypeArgument.INSTANCE));
        } else {
            Variance v;
            if (ch == '+') {
                pc.pos++;
                v = Variance.COVARIANT;
            } else if (ch == '-') {
                pc.pos++;
                v = Variance.CONTRAVARIANT;
            } else {
                v = Variance.INVARIANT;
            }
            final ReferenceTypeSignature nested = parseReferenceTypeSignature(cache, signature, pc);
            return parseClassTypeSignatureArgs(cache, signature, pc, cache.getTypeSignature(delegate, cache.getBoundTypeArgument(v, nested)));
        }
    }

    static ClassDeclarationSignature parseClassDeclarationSignature(final String signature) {
        final ParsingCache parsingCache = ParsingCache.get();
        final ParsingContext pc = PC_TL.get();
        int old = pc.pos;
        try {
            return parseClassDeclarationSignature(parsingCache, signature, pc);
        } finally {
            pc.pos = old;
        }
    }

    static ClassDeclarationSignature parseClassDeclarationSignature(final ParsingCache parsingCache, final String signature, final ParsingContext pc) {
        char ch;
        ch = signature.charAt(pc.pos);
        // unfortunately we need a temporary array
        TypeParameter[] params;
        if (ch == '<') {
            pc.pos++;
            // begin type parameters
            params = parseTypeParameters(parsingCache, signature, pc, 0);
        } else {
            params = NO_PARAMS;
        }
        ClassTypeSignature superclassSig = parseReferenceTypeSignature(parsingCache, signature, pc).asClass();
        ClassDeclarationSignature sig = parsingCache.getCachedClassDeclarationSignature(superclassSig);
        while (pc.pos < signature.length()) {
            final ClassTypeSignature interfaceSig = parseReferenceTypeSignature(parsingCache, signature, pc).asClass();
            sig = parsingCache.getCachedClassDeclarationSignatureWithInterface(sig, interfaceSig);
        }
        for (TypeParameter param : params) {
            sig = parsingCache.getCachedClassDeclarationSignatureWithParameter(sig, param);
        }
        return sig;
    }

    static TypeParameter[] parseTypeParameters(final ParsingCache parsingCache, final String signature, final ParsingContext pc, final int idx) {
        TypeParameter param = parseTypeParameter(parsingCache, signature, pc);
        TypeParameter[] array;
        if (signature.charAt(pc.pos) == '>') {
            pc.pos++;
            array = new TypeParameter[idx + 1];
        } else {
            array = parseTypeParameters(parsingCache, signature, pc, idx + 1);
        }
        array[idx] = param;
        return array;
    }

    static TypeParameter parseTypeParameter(final ParsingCache parsingCache, final String signature, final ParsingContext pc) {
        int start = pc.pos;
        char ch = signature.charAt(start);
        while (ch != ':') {
            pc.pos ++;
            ch = signature.charAt(pc.pos);
        }
        String identifier = parsingCache.getCachedName(signature, start, pc.pos);
        pc.pos++; // past the class bound leading :
        TypeParameter tp = parsingCache.getCachedTypeParameter(identifier);
        // class bound
        ch = signature.charAt(pc.pos);
        if (ch == '>') {
            // no class bounds and no further type parameters
            pc.pos++;
            return tp;
        }
        if (ch != ':') {
            // there's a class bound; parse it
            tp = parsingCache.getCachedTypeParameterWithClassBound(tp, parseReferenceTypeSignature(parsingCache, signature, pc));
            ch = signature.charAt(pc.pos);
        }
        // first char after class bound
        while (ch == ':') {
            pc.pos++;
            tp = parsingCache.getCachedTypeParameterWithInterfaceBound(tp, parseReferenceTypeSignature(parsingCache, signature, pc));
            ch = signature.charAt(pc.pos);
        }
        if (ch != '>') {
            throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
        }
        return tp;
    }

    static MethodDeclarationSignature parseMethodDeclarationSignature(final String signature) {
        final ParsingCache parsingCache = ParsingCache.get();
        final ParsingContext pc = PC_TL.get();
        int old = pc.pos;
        try {
            return parseMethodDeclarationSignature(parsingCache, signature, pc);
        } finally {
            pc.pos = old;
        }
    }

    static MethodDeclarationSignature parseMethodDeclarationSignature(final ParsingCache parsingCache, final String signature, final ParsingContext pc) {
        char ch;
        ch = signature.charAt(pc.pos);
        // unfortunately we need a temporary array
        TypeParameter[] params;
        if (ch == '<') {
            pc.pos++;
            // begin type parameters
            params = parseTypeParameters(parsingCache, signature, pc, 0);
        } else {
            params = NO_PARAMS;
        }
        ch = signature.charAt(pc.pos);
        if (ch != '(') {
            throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
        }
        pc.pos++;
        MethodDeclarationSignature ms = RootMethodDeclarationSignature.INSTANCE;
        while (signature.charAt(pc.pos) != ')') {
            final TypeSignature argSig = parseTypeSignature(parsingCache, signature, pc);
            ms = parsingCache.getMethodSignatureWithParamSignature(ms, argSig);
        }
        pc.pos ++;
        if (signature.charAt(pc.pos) != 'V') {
            final TypeSignature retSig = parseTypeSignature(parsingCache, signature, pc);
            ms = parsingCache.getMethodSignatureWithReturnType(ms, retSig);
        } else {
            pc.pos ++;
        }
        while (pc.pos < signature.length()) {
            if (signature.charAt(pc.pos) != '^') {
                throw new IllegalArgumentException("Unrecognized character at index " + pc.pos);
            }
            pc.pos ++;
            final ThrowableTypeSignature throwable = parseReferenceTypeSignature(parsingCache, signature, pc).asThrowable();
            ms = parsingCache.getMethodSignatureWithThrowable(ms, throwable);
        }
        for (TypeParameter param : params) {
            ms = parsingCache.getMethodSignatureWithTypeParameter(ms, param);
        }
        return ms;
    }

    static final class ParsingContext {
        int pos;
    }
}
