package org.qbicc.machine.probe;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qbicc.context.DiagnosticContext;
import org.qbicc.context.Location;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.CCompilerInvoker;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.CompilationFailureException;
import org.qbicc.machine.tool.ToolInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import io.smallrye.common.constraint.Assert;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;

/**
 * The universal C compiler probe utility.  Produces a single C source file that is compiled to an object file
 * which is analyzed to determine information about the target environment.
 */
public final class CProbe {
    private static final byte[] NO_BYTES = new byte[0];
    private final List<Step> items;
    private final List<Type> types;
    private final List<String> constants;
    private final Map<String, Type> constantTypes;
    private final List<String> functionNames;

    CProbe(final Builder builder) {
        this.items = List.copyOf(builder.items);
        this.types = List.copyOf(builder.types);
        this.constants = List.copyOf(builder.constants);
        this.constantTypes = Map.copyOf(builder.constantTypes);
        this.functionNames = List.copyOf(builder.functionNames);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Run the probe.
     *
     * @param toolChain the C tool chain to use (must not be {@code null})
     * @param objectFileProvider the object file provider to use (must not be {@code null})
     * @param errorReporter the context to report errors to, or {@code null} to skip error reporting
     * @return the result, or {@code null} if the compilation failed
     * @throws IOException if communications with or execution of the compiler failed
     */
    public Result run(CToolChain toolChain, ObjectFileProvider objectFileProvider, DiagnosticContext errorReporter) throws IOException {
        final CCompilerInvoker inv = toolChain.newCompilerInvoker();
        StringBuilder b = new StringBuilder();
        for (Step item : items) {
            item.appendTo(b);
        }
        inv.setSource(InputSource.from(b));
        final Path path = Files.createTempFile("qbicc-probe-", "." + objectFileProvider.getObjectType().objectSuffix());
        try (Closeable c = ProbeUtil.deleting(path)) {
            inv.setOutputPath(path);
            if (errorReporter != null) {
                inv.setMessageHandler(new ToolMessageHandler() {
                    public void handleMessage(final ToolInvoker invoker, final Level level, final String file, final int line, final int column, final String message) {
                        if (level == Level.ERROR) {
                            errorReporter.error(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", invoker.getTool().getToolName(), message);
                        } else if (level == Level.WARNING) {
                            errorReporter.warning(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", invoker.getTool().getToolName(), message);
                        } else if (level == Level.INFO) {
                            errorReporter.note(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", invoker.getTool().getToolName(), message);
                        }
                    }
                });
            }
            try {
                inv.invoke();
            } catch (CompilationFailureException e) {
                // no result
                return null;
            }
            // analyze the result
            try (final ObjectFile objectFile = objectFileProvider.openObjectFile(path)) {
                int cnt = constants.size();
                ByteOrder byteOrder = objectFile.getByteOrder();
                final Map<String, ConstantInfo> constantInfos = new HashMap<>(cnt);
                for (int i = 0; i < cnt; i ++) {
                    String name = constants.get(i);
                    boolean defined = objectFile.getSymbolValueAsByte("cp_is_defined" + i) != 0;
                    int size = (int) objectFile.getSymbolValueAsLong("cp_size" + i);
                    boolean signed = objectFile.getSymbolValueAsByte("cp_is_signed" + i) != 0;
                    boolean unsigned = objectFile.getSymbolValueAsByte("cp_is_unsigned" + i) != 0;
                    boolean floating = objectFile.getSymbolValueAsByte("cp_is_floating" + i) != 0;
                    boolean bool = objectFile.getSymbolValueAsByte("cp_is_bool" + i) != 0;
                    constantInfos.put(name, new ConstantInfo(defined, objectFile.getSymbolAsBytes("cp_value" + i, size), name, byteOrder, signed, unsigned, floating, bool));
                }
                cnt = functionNames.size();
                final Map<String, FunctionInfo> functionInfos = new HashMap<>(cnt);
                for (int i = 0; i < cnt; i ++) {
                    String name = functionNames.get(i);
                    int size = (int) objectFile.getSymbolValueAsLong("fn_name_size" + i) - 1; // -1 to exclude the '\0' character
                    String stringValue = objectFile.getSymbolValueAsUtfString("fn_name" + i, size);
                    functionInfos.put(name, new FunctionInfo(stringValue));
                }
                cnt = types.size();
                final Map<Type, Type.Info> typeInfos = new HashMap<>(cnt);
                final Map<Type, Map<String, Type.Info>> memberInfos = new HashMap<>();
                for (int i = 0; i < cnt; i ++) {
                    Type type = types.get(i);
                    long overallSize = objectFile.getSymbolValueAsLong("tp_overall_size" + i);
                    long overallAlign = objectFile.getSymbolValueAsLong("tp_overall_align" + i);
                    boolean signed = objectFile.getSymbolValueAsByte("tp_is_signed" + i) != 0;
                    boolean unsigned = objectFile.getSymbolValueAsByte("tp_is_unsigned" + i) != 0;
                    boolean floating = objectFile.getSymbolValueAsByte("tp_is_floating" + i) != 0;
                    Type.Info info = new Type.Info(overallSize, overallAlign, 0, signed, unsigned, floating);
                    Map<String, Type.Info> memberInfo = new HashMap<>(type.getMembers().size());
                    for (String memberName : type.getMembers()) {
                        long memberSize = objectFile.getSymbolValueAsLong("tp_sizeof_" + memberName + i);
                        long memberOffset = objectFile.getSymbolValueAsLong("tp_offsetof_" + memberName + i);
                        boolean memberSigned = objectFile.getSymbolValueAsByte("tp_is_signed_" + memberName + i) != 0;
                        boolean memberUnsigned = objectFile.getSymbolValueAsByte("tp_is_unsigned_" + memberName + i) != 0;
                        boolean memberFloating = objectFile.getSymbolValueAsByte("tp_is_floating_" + memberName + i) != 0;
                        memberInfo.put(memberName, new Type.Info(memberSize, 0, memberOffset, memberSigned, memberUnsigned, memberFloating));
                    }
                    typeInfos.put(type, info);
                    memberInfos.put(type, memberInfo);
                }
                return new Result(typeInfos, memberInfos, functionInfos, constantInfos, byteOrder);
            }
        }
    }


    public static final class Builder {
        private final List<Step> items = new ArrayList<>();
        private final List<Type> types = new ArrayList<>();
        private final List<String> constants = new ArrayList<>();
        private final Map<String, Type> constantTypes = new HashMap<>();
        private final List<String> functionNames = new ArrayList<>();

        Builder() {
            // XXX - needed for offsetof; maybe use __builtin_offsetof instead? custom macro?
            include("<stddef.h>");
            // XXX - needed for CHAR_MIN/SCHAR_MIN
            include("<limits.h>");
        }

        // top level steps

        public Builder include(String include) {
            if (include.startsWith("<") && include.endsWith(">")) {
                items.add(new Include(include.substring(1, include.length() - 1), false));
            } else {
                items.add(new Include(include, true));
            }
            return this;
        }

        public Builder define(String key) {
            items.add(new Define(key, null));
            return this;
        }

        public Builder define(String key, String value) {
            items.add(new Define(key, value));
            return this;
        }

        public Builder line(int line, String file) {
            if (line > 0) {
                items.add(new Line(line, file));
            }
            return this;
        }

        public Builder line(int line) {
            return line(line, null);
        }

        public Builder probeType(Type type) {
            return probeType(type, null, 0);
        }

        /**
         * Probe the given type information.
         *
         * @param type the type to probe (must not be {@code null})
         * @param sourceFile the source file name where the type is declared (may be {@code null})
         * @param line the source line number or 0 if none
         * @return this builder
         */
        public Builder probeType(Type type, String sourceFile, int line) {
            Assert.checkNotNullParam("type", type);
            TypeStep typeStep = getTypeStepOf(type);
            int idx = types.size();
            ValueStep probeVal = deref(zeroPtrTo(typeStep));
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "tp_is_signed", idx, isSigned(probeVal)));
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "tp_is_unsigned", idx, isUnsigned(probeVal)));
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "tp_is_floating", idx, isFloating(probeVal)));
            line(line, sourceFile);
            add(decl(NamedType.UNSIGNED_LONG, "tp_overall_size", idx, sizeof(typeStep)));
            line(line, sourceFile);
            add(decl(NamedType.UNSIGNED_LONG, "tp_overall_align", idx, alignof(typeStep)));
            for (String memberName : type.getMembers()) {
                line(line, sourceFile);
                ValueStep member = memberOf(typeStep, memberName);
                add(decl(NamedType.UNSIGNED_LONG, "tp_sizeof_" + memberName, idx, sizeof(member)));
                line(line, sourceFile);
                add(decl(NamedType.UNSIGNED_LONG, "tp_offsetof_" + memberName, idx, offsetOf(typeStep, memberName)));
                line(line, sourceFile);
                add(decl(NamedType.BOOL, "tp_is_signed_" + memberName, idx, isSigned(member)));
                line(line, sourceFile);
                add(decl(NamedType.BOOL, "tp_is_unsigned_" + memberName, idx, isUnsigned(member)));
                line(line, sourceFile);
                add(decl(NamedType.BOOL, "tp_is_floating_" + memberName, idx, isFloating(member)));
            }
            types.add(type);
            return this;
        }

        public Builder probeConstant(String name) {
            return probeConstant(name, null, 0);
        }

        /**
         * Probe the given typeless constant value.
         *
         * @param name the constant name (must not be {@code null})
         * @param sourceFile the source file name where the type is declared (may be {@code null})
         * @param line the source line number or 0 if none
         * @return this builder
         */
        public Builder probeConstant(String name, String sourceFile, int line) {
            return probeConstant(name, null, sourceFile, line);
        }

        /**
         * Probe the given typed constant value.
         *
         * @param name the constant name (must not be {@code null})
         * @param type the constant type info
         * @param sourceFile the source file name where the type is declared (may be {@code null})
         * @param line the source line number or 0 if none
         * @return this builder
         */
        public Builder probeConstant(String name, Type type, String sourceFile, int line) {
            Assert.checkNotNullParam("name", name);
            int idx = constants.size();
            ValueStep symbol = identifier(name);
            if_(defined(symbol));
            // it's a macro
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "cp_is_defined", idx, Number.ONE));
            // type
            TypeStep typeStep = typeof(symbol);
            else_();
            // it's a real symbol
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "cp_is_defined", idx, Number.ZERO));
            endif();
            // get the actual value
            line(line, sourceFile);
            add(decl(typeStep, "cp_value", idx, type != null ? cast(symbol, typeStep) : symbol));
            // get the size of the value
            line(line, sourceFile);
            add(decl(NamedType.UNSIGNED_LONG, "cp_size", idx, sizeof(symbol)));
            // probe value information
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "cp_is_signed", idx, isSigned(symbol)));
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "cp_is_unsigned", idx, isUnsigned(symbol)));
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "cp_is_floating", idx, isFloating(symbol)));
            line(line, sourceFile);
            add(decl(NamedType.BOOL, "cp_is_bool", idx, isBool(symbol)));
            constants.add(name);
            return this;
        }

        public Builder probeMacroFunctionName(String name, String sourceFile, int line) {
            Assert.checkNotNullParam("name", name);
            int idx = functionNames.size();
            add(macro("QBICC_PASTE1", List.of("x"), "#x"));
            add(macro("QBICC_PASTE2", List.of("x"), "QBICC_PASTE1(x)"));
            Identifier fnName = new Identifier("fn_name" + idx);
            line(line, sourceFile);
            // work around to create array types - create a typedef
            add(typedef(NamedType.CHAR, "char_array[]"));
            NamedType charArray = new NamedType("char_array");
            line(line, sourceFile);
            add(decl(charArray, "fn_name", idx, call("QBICC_PASTE2", call(name, null))));
            line(line, sourceFile);
            add(decl(NamedType.UNSIGNED_LONG, "fn_name_size", idx, sizeof(fnName)));
            functionNames.add(name);
            return this;
        }

        public CProbe build() {
            return new CProbe(this);
        }

        private TypeStep getTypeStepOf(final Type type) {
            if (type.getQualifier() == Qualifier.NONE) {
                return namedType(type.getName());
            } else if (type.getQualifier() == Qualifier.STRUCT) {
                return tagType(Tag.STRUCT, type.getName());
            } else {
                assert type.getQualifier() == Qualifier.UNION;
                return tagType(Tag.UNION, type.getName());
            }
        }

        // add item

        Step add(Step step) {
            items.add(step);
            return step;
        }

        // preproc internal

        void if_(ValueStep cond) {
            items.add(new If(cond));
        }

        void elsif(ValueStep cond) {
            items.add(new ElsIf(cond));
        }

        void else_() {
            items.add(SimplePreProc.ELSE);
        }

        void endif() {
            items.add(SimplePreProc.ENDIF);
        }

        // atoms

        Step typedef(TypeStep type, String alias) {
            return new Typedef(type, alias);
        }

        Step decl(TypeStep type, String name, int idx, ValueStep value) {
            return new Decl(type, name, idx, value);
        }

        Step decl(TypeStep type, String name, ValueStep value) {
            return new Decl(type, name, -1, value);
        }

        Step macro(String key, List<String> arguments, String value) {
            return new Define(key, arguments, value);
        };

        <T extends Step> ListOf<T> listOf(List<T> items) {
            return new ListOf<>(items);
        }

        TypeStep ptrTo(TypeStep type) {
            return new PtrTo(type);
        }

        TypeStep arrayOf(TypeStep type) {
            return new ArrayOf(type);
        }

        ValueStep addrOf(ValueStep value) {
            return new AddrOf(value);
        }

        ValueStep deref(ValueStep value) {
            return new Deref(value);
        }

        ValueStep cast(ValueStep value, TypeStep toType) {
            return new Cast(value, toType);
        }

        ValueStep number(int number) {
            return new Number(number);
        }

        TypeStep namedType(String name) {
            return new NamedType(name);
        }

        TypeStep tagType(Tag tag, String name) {
            return new TagType(tag, name);
        }

        ValueStep generic(ValueStep object, ListOf<AssocItem> assocList) {
            return new Generic(object, assocList);
        }

        ValueStep eq(ValueStep v1, ValueStep v2) {
            return new Eq(v1, v2);
        }

        ValueStep call(String name, Step arg) {
            return new SimpleCall(name, arg);
        }

        ValueStep memberOfPtr(ValueStep ptr, String name) {
            return new MemberOfPtr(ptr, name);
        }

        ValueStep offsetOf(TypeStep compound, String name) {
            return new OffsetOf(compound, name);
        }

        AssocItem assocItem(TypeStep type, ValueStep expr) {
            return new AssocItem(type, expr);
        }

        ValueStep isDefined(ValueStep expr) {
            return new IfDefined(expr);
        }

        ValueStep isDefined(ValueStep expr, ValueStep trueVal, ValueStep falseVal) {
            return new IfDefined(expr, trueVal, falseVal);
        }

        ValueStep zeroStruct() {
            return ZeroStruct.INSTANCE;
        }

        ValueStep identifier(String identifier) {
            return new Identifier(identifier);
        }

        TypeStep typeof(ValueStep object) {
            return new TypeOf(object);
        }

        // intermediates

        ValueStep zeroPtrTo(TypeStep type) {
            return cast(Number.ZERO, ptrTo(type));
        }

        ValueStep memberOf(TypeStep type, String name) {
            return memberOfPtr(zeroPtrTo(type), name);
        }

        ValueStep sizeof(Step object) {
            return call("sizeof", object);
        }

        ValueStep alignof(Step object) {
            return call("_Alignof", object);
        }

        ValueStep defined(ValueStep expr) {
            return call("defined", expr);
        }

        ValueStep isFloating(ValueStep expr) {
            return generic(expr, listOf(List.of(
                assocItem(NamedType.FLOAT, Number.ONE),
                assocItem(NamedType.DOUBLE, Number.ONE),
                assocItem(NamedType.LONG_DOUBLE, Number.ONE),
                assocItem(NamedType.DEFAULT, Number.ZERO)
            )));
        }

        ValueStep isUnsigned(ValueStep expr) {
            return generic(expr, listOf(List.of(
                assocItem(NamedType.CHAR, eq(Identifier.CHAR_MIN, Number.ZERO)),
                assocItem(NamedType.UNSIGNED_CHAR, Number.ONE),
                assocItem(NamedType.UNSIGNED_SHORT, Number.ONE),
                assocItem(NamedType.UNSIGNED_INT, Number.ONE),
                assocItem(NamedType.UNSIGNED_LONG, Number.ONE),
                assocItem(NamedType.UNSIGNED_LONG_LONG, Number.ONE),
                assocItem(NamedType.DEFAULT, Number.ZERO)
            )));
        }

        ValueStep isSigned(ValueStep expr) {
            return generic(expr, listOf(List.of(
                assocItem(NamedType.CHAR, eq(Identifier.CHAR_MIN, Identifier.SCHAR_MIN)),
                assocItem(NamedType.SIGNED_CHAR, Number.ONE),
                assocItem(NamedType.SIGNED_SHORT, Number.ONE),
                assocItem(NamedType.SIGNED_INT, Number.ONE),
                assocItem(NamedType.SIGNED_LONG, Number.ONE),
                assocItem(NamedType.SIGNED_LONG_LONG, Number.ONE),
                assocItem(NamedType.DEFAULT, Number.ZERO)
            )));
        }

        ValueStep isBool(ValueStep expr) {
            return generic(expr, listOf(List.of(
                assocItem(NamedType.BOOL, Number.ONE),
                assocItem(NamedType.DEFAULT, Number.ZERO)
            )));
        }


    }

    /**
     * A description of a type to probe.
     */
    public static final class Type {
        private final String name;
        private final Qualifier qualifier;
        private final List<String> members;
        private final int hashCode;

        Type(Type.Builder builder) {
            name = Assert.checkNotNullParam("builder.name", builder.name);
            qualifier = builder.qualifier;
            List<String> members = builder.members;
            if (members != null) {
                this.members = List.copyOf(builder.members);
            } else {
                this.members = List.of();
            }
            hashCode = Objects.hash(name, qualifier, members);
        }

        public String getName() {
            return name;
        }

        public Qualifier getQualifier() {
            return qualifier;
        }

        public List<String> getMembers() {
            return members;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Type && equals((Type) obj);
        }

        public boolean equals(final Type other) {
            return this == other || other != null
                && hashCode == other.hashCode
                && name.equals(other.name)
                && qualifier == other.qualifier
                && members.equals(other.members);
        }

        public static Type.Builder builder() {
            return new Type.Builder();
        }

        public static final class Builder {
            private String name;
            private Qualifier qualifier = Qualifier.NONE;
            private List<String> members;

            Builder() {}

            public String getName() {
                return name;
            }

            public Type.Builder setName(final String name) {
                this.name = Assert.checkNotNullParam("name", name);
                return this;
            }

            public Qualifier getQualifier() {
                return qualifier;
            }

            public Type.Builder setQualifier(final Qualifier qualifier) {
                this.qualifier = Assert.checkNotNullParam("qualifier", qualifier);
                return this;
            }

            public Type.Builder addMember(String name) {
                List<String> members = this.members;
                if (members == null) {
                    this.members = members = new ArrayList<>();
                }
                members.add(name);
                return this;
            }

            public Type build() {
                return new Type(this);
            }
        }

        public static final class Info {
            private final long size;
            private final long align;
            private final long offset;
            private final boolean signed;
            private final boolean unsigned;
            private final boolean floating;

            Info(final long size, final long align, final long offset, final boolean signed, final boolean unsigned, final boolean floating) {
                this.size = size;
                this.align = align;
                this.offset = offset;
                this.signed = signed;
                this.unsigned = unsigned;
                this.floating = floating;
            }

            public long getSize() {
                return size;
            }

            public long getAlign() {
                return align;
            }

            public long getOffset() {
                return offset;
            }

            public boolean isSigned() {
                return signed;
            }

            public boolean isUnsigned() {
                return unsigned;
            }

            public boolean isFloating() {
                return floating;
            }
        }
    }

    /**
     * The probe result.
     */
    public static final class Result {
        private final Map<Type, Type.Info> typeInfos;
        private final Map<Type, Map<String, Type.Info>> memberInfos;
        private final Map<String, FunctionInfo> functionInfos;
        private final Map<String, ConstantInfo> constantInfos;
        private final ByteOrder byteOrder;

        Result(final Map<Type, Type.Info> typeInfos, final Map<Type, Map<String, Type.Info>> memberInfos, final Map<String, FunctionInfo> functionInfos, final Map<String, ConstantInfo> constantInfos, ByteOrder byteOrder) {
            this.typeInfos = typeInfos;
            this.memberInfos = memberInfos;
            this.functionInfos = functionInfos;
            this.constantInfos = constantInfos;
            this.byteOrder = byteOrder;
        }

        public Type.Info getTypeInfo(Type type) throws NoSuchElementException {
            Type.Info info = typeInfos.get(type);
            if (info == null) {
                throw new NoSuchElementException();
            }
            return info;
        }

        public Type.Info getTypeInfoOfMember(Type type, String memberName) throws NoSuchElementException {
            Map<String, Type.Info> map = memberInfos.get(type);
            if (map == null) {
                throw new NoSuchElementException();
            }
            Type.Info info = map.get(memberName);
            if (info == null) {
                throw new NoSuchElementException();
            }
            return info;
        }

        public FunctionInfo getFunctionInfo(String funcName) throws NoSuchElementException {
            FunctionInfo val = functionInfos.get(funcName);
            if (val == null) {
                throw new NoSuchElementException();
            }
            return val;
        }

        public ConstantInfo getConstantInfo(String constant) throws NoSuchElementException {
            ConstantInfo val = constantInfos.get(constant);
            if (val == null) {
                throw new NoSuchElementException();
            }
            return val;
        }

        public ByteOrder getByteOrder() {
            return byteOrder;
        }
    }

    public static final class ConstantInfo {
        private final boolean defined;
        private final byte[] value;
        private final String symbol;
        private final ByteOrder byteOrder;
        private final boolean signed;
        private final boolean unsigned;
        private final boolean floating;
        private final boolean bool;

        ConstantInfo(final boolean defined, final byte[] value, final String symbol, final ByteOrder byteOrder, boolean signed, boolean unsigned, boolean floating, boolean bool) {
            this.defined = defined;
            this.value = value;
            this.symbol = symbol;
            this.byteOrder = byteOrder;
            this.signed = signed;
            this.unsigned = unsigned;
            this.floating = floating;
            this.bool = bool;
        }

        public boolean isDefined() {
            return defined;
        }

        public boolean hasValue() {
            return value != null;
        }

        public byte[] getValue() {
            return value;
        }

        public boolean hasSymbol() {
            return symbol != null;
        }

        public int getSize() {
            return value.length;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getValueAsInt() {
            return (int) getValueAsUnsignedLong();
        }

        public long getValueAsSignedLong() {
            ByteBuffer buf = ByteBuffer.wrap(value).order(byteOrder);
            if (buf.remaining() >= 8) {
                return buf.getLong();
            } else if (buf.remaining() >= 4) {
                return buf.getInt();
            } else if (buf.remaining() >= 2) {
                return buf.getShort();
            } else if (buf.remaining() >= 1) {
                return buf.get();
            } else {
                return 0;
            }
        }

        public long getValueAsUnsignedLong() {
            ByteBuffer buf = ByteBuffer.wrap(value).order(byteOrder);
            if (buf.remaining() >= 8) {
                return buf.getLong();
            } else if (buf.remaining() >= 4) {
                return buf.getInt() & 0xffff_ffffL;
            } else if (buf.remaining() >= 2) {
                return buf.getShort() & 0xffff;
            } else if (buf.remaining() >= 1) {
                return buf.get() & 0xff;
            } else {
                return 0;
            }
        }

        public Literal getValueAsLiteralOfType(TypeSystem ts, LiteralFactory lf, ValueType valueType) {
            if (valueType instanceof SignedIntegerType) {
                return lf.literalOf((SignedIntegerType) valueType, getValueAsSignedLong());
            } else if (valueType instanceof UnsignedIntegerType) {
                return lf.literalOf((UnsignedIntegerType) valueType, getValueAsUnsignedLong());
            } else if (valueType instanceof BooleanType) {
                return lf.literalOf(getValueAsUnsignedLong() != 0);
            } else if (valueType instanceof ArrayType) {
                return lf.literalOf((ArrayType) valueType, value);
            } else {
                /* try again with the constant's natural type if one of these types is not specified. */
                return getValueAsLiteral(ts, lf);
            }
        }

        public Literal getValueAsLiteral(TypeSystem ts, LiteralFactory lf) {
            ValueType valueType = getValueType(ts);
            if (valueType instanceof SignedIntegerType) {
                return lf.literalOf((SignedIntegerType) valueType, getValueAsSignedLong());
            } else if (valueType instanceof UnsignedIntegerType) {
                return lf.literalOf((UnsignedIntegerType) valueType, getValueAsUnsignedLong());
            } else if (valueType instanceof BooleanType) {
                return lf.literalOf(getValueAsUnsignedLong() != 0);
            } else if (valueType instanceof ArrayType) {
                return lf.literalOf((ArrayType) valueType, value);
            } else {
                throw new IllegalArgumentException("Invalid constant type");
            }
        }

        public ValueType getValueType(TypeSystem ts) {
            int size = value.length;
            if (signed) {
                if (size == 1) {
                    return ts.getSignedInteger8Type();
                } else if (size == 2) {
                    return ts.getSignedInteger16Type();
                } else if (size == 4) {
                    return ts.getSignedInteger32Type();
                } else if (size == 8) {
                    return ts.getSignedInteger64Type();
                }
            } else if (unsigned) {
                if (size == 1) {
                    return ts.getUnsignedInteger8Type();
                } else if (size == 2) {
                    return ts.getUnsignedInteger16Type();
                } else if (size == 4) {
                    return ts.getUnsignedInteger32Type();
                } else if (size == 8) {
                    return ts.getUnsignedInteger64Type();
                }
            } else if (floating) {
                if (size == 4) {
                    return ts.getFloat32Type();
                } else if (size == 8) {
                    return ts.getFloat64Type();
                }
            } else if (bool) {
                return ts.getBooleanType();
            } else {
                return ts.getArrayType(ts.getUnsignedInteger8Type(), size);
            }
            throw new IllegalArgumentException("Unable to determine type of constant");
        }
    }

    public static final class FunctionInfo {
        private final String resolvedName;

        FunctionInfo(String resolvedName) {
            this.resolvedName = resolvedName;
        }

        public String getResolvedName() {
            // check if the obtained value matches the expected pattern of function name
            Pattern namePattern = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)[(][)]");
            Matcher matcher = namePattern.matcher(resolvedName);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Unexpected function name pattern after macro expansion");
            }
            return matcher.group(1);
        }
    }


    // private

    static abstract class Step {

        /**
         * Append the step with trailing newline.
         *
         * @param b the string builder
         * @return the same builder
         */
        abstract StringBuilder appendTo(StringBuilder b);

        final StringBuilder nl(StringBuilder b) {
            return b.append(System.lineSeparator());
        }
    }

    static abstract class PreProc extends Step {
    }

    static final class Include extends PreProc {
        private final String name;
        private final boolean quotes;

        Include(final String name, final boolean quotes) {
            this.name = name;
            this.quotes = quotes;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return nl(b
                .append('#').append("include")
                .append(' ')
                .append(quotes ? '"' : '<')
                .append(name)
                .append(quotes ? '"' : '>')
            );
        }
    }

    static final class Define extends PreProc {
        private final String key;
        private final List<String> arguments;
        private final String value;

        Define(final String key, final String value) {
            this(key, List.of(), value);
        }

        Define(final String key, final List<String> arguments, final String value) {
            this.key = key;
            this.arguments = arguments;
            this.value = value;
        }

        StringBuilder appendTo(final StringBuilder b) {
            b.append('#').append("define").append(' ').append(key);
            Iterator iterator = arguments.iterator();
            if (iterator.hasNext()) {
                b.append('(');
                b.append(iterator.next());
                while (iterator.hasNext()) {
                    b.append(',');
                    b.append(iterator.next());
                }
                b.append(')');
            }
            if (value != null && ! value.isEmpty()) {
                b.append(' ').append(value);
            }
            return nl(b);
        }
    }

    static final class Line extends PreProc {
        private final int line;
        private final String file;

        Line(final int line, final String file) {
            this.line = line;
            this.file = file;
        }

        StringBuilder appendTo(final StringBuilder b) {
            b.append('#').append("line").append(' ').append(line);
            if (file != null) {
                b.append(' ').append('"').append(file).append('"');
            }
            return nl(b);
        }
    }

    static final class If extends PreProc {
        private final ValueStep cond;

        If(final ValueStep cond) {
            this.cond = cond;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return nl(cond.appendTo(b.append('#').append("if").append(' ')));
        }
    }

    static final class ElsIf extends PreProc {
        private final ValueStep cond;

        ElsIf(final ValueStep cond) {
            this.cond = cond;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return nl(cond.appendTo(b.append('#').append("elsif").append(' ')));
        }
    }

    static final class SimplePreProc extends PreProc {
        static final SimplePreProc ELSE = new SimplePreProc("else");
        static final SimplePreProc ENDIF = new SimplePreProc("endif");

        private final String keyWord;

        SimplePreProc(final String keyWord) {
            this.keyWord = keyWord;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return nl(b.append('#').append(keyWord));
        }
    }

    // plain steps

    static final class ListOf<T extends Step> extends Step {
        private final List<T> steps;

        ListOf(final List<T> steps) {
            this.steps = steps;
        }

        @SafeVarargs
        static <T extends Step> ListOf<T> steps(T... step) {
            return new ListOf<>(List.of(step));
        }

        StringBuilder appendTo(final StringBuilder b) {
            Iterator<T> iterator = steps.iterator();
            if (iterator.hasNext()) {
                iterator.next().appendTo(b);
                while (iterator.hasNext()) {
                    iterator.next().appendTo(b.append(','));
                }
            }
            return b;
        }
    }

    static final class AssocItem extends Step {
        private final Step type;
        private final ValueStep expr;

        AssocItem(final Step type, final ValueStep expr) {
            this.type = type;
            this.expr = expr;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return expr.appendTo(type.appendTo(b).append(':').append(' '));
        }
    }

    static abstract class TypeStep extends Step {}

    static final class NamedType extends TypeStep {
        // special case
        static final NamedType DEFAULT = new NamedType("default");

        static final NamedType VOID = new NamedType("void");

        static final NamedType BOOL = new NamedType("_Bool");

        static final NamedType FLOAT = new NamedType("float");
        static final NamedType DOUBLE = new NamedType("double");
        static final NamedType LONG_DOUBLE = new NamedType("long double");

        static final NamedType CHAR = new NamedType("char");

        static final NamedType UNSIGNED_CHAR = new NamedType("unsigned char");
        static final NamedType UNSIGNED_SHORT = new NamedType("unsigned short");
        static final NamedType UNSIGNED_INT = new NamedType("unsigned int");
        static final NamedType UNSIGNED_LONG = new NamedType("unsigned long");
        static final NamedType UNSIGNED_LONG_LONG = new NamedType("unsigned long long");

        static final NamedType SIGNED_CHAR = new NamedType("signed char");
        static final NamedType SIGNED_SHORT = new NamedType("signed short");
        static final NamedType SIGNED_INT = new NamedType("signed int");
        static final NamedType SIGNED_LONG = new NamedType("signed long");
        static final NamedType SIGNED_LONG_LONG = new NamedType("signed long long");

        private final String name;

        NamedType(final String name) {
            this.name = name;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return b.append(name);
        }
    }

    static final class TagType extends TypeStep {
        private final Tag tag;
        private final String name;

        TagType(final Tag tag, final String name) {
            this.tag = tag;
            this.name = name;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return b.append(tag).append(' ').append(name);
        }
    }

    enum Tag {
        STRUCT,
        UNION,
        ENUM,
        ;
        private final String str = name().toLowerCase(Locale.ROOT);

        public String toString() {
            return str;
        }
    }

    static final class PtrTo extends TypeStep {
        private final TypeStep type;

        PtrTo(final TypeStep type) {
            this.type = type;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return type.appendTo(b).append(' ').append('*');
        }
    }

    static final class ArrayOf extends TypeStep {
        private final TypeStep type;

        ArrayOf(final TypeStep type) {
            this.type = type;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return type.appendTo(b).append(' ').append('[').append(']');
        }
    }

    static abstract class ValueStep extends Step {}

    static final class Number extends ValueStep {
        static final Number ZERO = new Number(0);
        static final Number ONE = new Number(1);
        static final Number M_ONE = new Number(-1);

        private final long val;

        Number(final long val) {
            this.val = val;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return b.append(val);
        }
    }

    static final class Identifier extends ValueStep {
        static final Identifier CHAR_MIN = new Identifier("CHAR_MIN");
        static final Identifier SCHAR_MIN = new Identifier("SCHAR_MIN");

        private final String name;

        Identifier(final String name) {
            this.name = name;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return b.append(name);
        }
    }

    static final class Cast extends ValueStep {
        private final ValueStep value;
        private final TypeStep toType;

        Cast(final ValueStep value, final TypeStep toType) {
            this.value = value;
            this.toType = toType;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return value.appendTo(toType.appendTo(b.append('(').append('(')).append(')')).append(')');
        }
    }

    static final class Generic extends ValueStep {
        private final ValueStep object;
        private final ListOf<AssocItem> items;

        Generic(final ValueStep object, final ListOf<AssocItem> items) {
            this.object = object;
            this.items = items;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return items.appendTo(object.appendTo(b.append("_Generic").append('(')).append(',')).append(')');
        }
    }

    static final class Eq extends ValueStep {
        private final ValueStep v1;
        private final ValueStep v2;

        Eq(final ValueStep v1, final ValueStep v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return v2.appendTo(v1.appendTo(b).append(' ').append("==").append(' '));
        }
    }

    static final class SimpleCall extends ValueStep {
        private final String name;
        private final Step value;

        SimpleCall(final String name, final Step value) {
            this.name = name;
            this.value = value;
        }

        StringBuilder appendTo(final StringBuilder b) {
            if (value == null) {
                return b.append(name).append('(').append(')');
            } else {
                return value.appendTo(b.append(name).append('(')).append(')');
            }
        }
    }

    static final class TypeOf extends TypeStep {
        private final ValueStep value;

        TypeOf(final ValueStep value) {
            this.value = value;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return value.appendTo(b.append("typeof").append('(')).append(')');
        }
    }

    static final class AddrOf extends ValueStep {
        private final ValueStep value;

        AddrOf(final ValueStep value) {
            this.value = value;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return value.appendTo(b.append('&').append('(')).append(')');
        }
    }

    static final class Deref extends ValueStep {
        private final ValueStep value;

        Deref(final ValueStep value) {
            this.value = value;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return value.appendTo(b.append('*').append('(')).append(')');
        }
    }

    static final class MemberOfPtr extends ValueStep {
        private final ValueStep value;
        private final String memberName;

        MemberOfPtr(final ValueStep value, final String memberName) {
            this.value = value;
            this.memberName = memberName;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return value.appendTo(b.append('(')).append("->").append(memberName).append(')');
        }
    }

    static final class ZeroStruct extends ValueStep {
        static final ZeroStruct INSTANCE = new ZeroStruct();

        private ZeroStruct() {}

        StringBuilder appendTo(final StringBuilder b) {
            return b.append('{').append('0').append('}');
        }
    }

    static final class OffsetOf extends ValueStep {
        private final TypeStep type;
        private final String memberName;

        OffsetOf(final TypeStep type, final String memberName) {
            this.type = type;
            this.memberName = memberName;
        }

        StringBuilder appendTo(final StringBuilder b) {
            return type.appendTo(b.append("offsetof").append('(')).append(',').append(memberName).append(')');
        }
    }

    static final class IfDefined extends ValueStep {
        private final ValueStep value;
        private final ValueStep trueVal;
        private final ValueStep falseVal;

        IfDefined(final ValueStep value) {
            this(value, Number.ONE, Number.ZERO);
        }

        IfDefined(final ValueStep value, final ValueStep trueVal, final ValueStep falseVal) {
            this.value = value;
            this.trueVal = trueVal;
            this.falseVal = falseVal;
        }

        StringBuilder appendTo(final StringBuilder b) {
            nl(b);
            b.append('#').append("if").append(' ').append("defined").append('(');
            value.appendTo(b);
            b.append(')');
            nl(b);
            trueVal.appendTo(b);
            nl(b);
            b.append('#').append("else");
            nl(b);
            falseVal.appendTo(b);
            nl(b);
            b.append('#').append("endif");
            nl(b);
            return b;
        }
    }

    static final class Typedef extends Step {
        private final TypeStep type;
        private final String alias;

        public Typedef(TypeStep type, String alias) {
            this.type = type;
            this.alias = alias;
        }

        @Override
        StringBuilder appendTo(StringBuilder b) {
            return nl(type.appendTo(b.append("typedef").append(' ')).append(' ').append(alias).append(';'));
        }
    }

    static final class Decl extends Step {
        private final TypeStep type;
        private final String name;
        private final int index;
        private final ValueStep value;

        Decl(final TypeStep type, final String name, final int index, final ValueStep value) {
            this.type = type;
            this.name = name;
            this.index = index;
            this.value = value;
        }

        StringBuilder appendTo(final StringBuilder b) {
            type.appendTo(b).append(' ').append(name);
            if (index >= 0) {
                b.append(index);
            }
            // this is ugly but would work for now
            if (type instanceof ArrayOf) {
                b.append("[]");
            }
            return nl(value.appendTo(b.append(' ').append('=').append(' ')).append(';'));
        }
    }

    static abstract class SubProbe extends Step {

    }

}
