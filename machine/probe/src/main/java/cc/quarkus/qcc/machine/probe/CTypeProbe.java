package cc.quarkus.qcc.machine.probe;

import static cc.quarkus.qcc.machine.probe.ProbeUtil.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.UnaryOperator;

import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CCompilerInvoker;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class CTypeProbe {
    private final List<String> includes;
    private final Map<String, String> defines;
    private final List<Type> types;

    CTypeProbe(Builder builder) {
        List<String> includes = builder.includes;
        if (includes != null) {
            this.includes = List.copyOf(includes);
        } else {
            this.includes = List.of();
        }
        Map<String, String> defines = builder.defines;
        if (defines != null) {
            this.defines = Map.copyOf(defines);
        } else {
            this.defines = Map.of();
        }
        this.types = List.copyOf(builder.types);
    }

    public List<String> getIncludes() {
        return includes;
    }

    public Map<String, String> getDefines() {
        return defines;
    }

    public List<Type> getTypes() {
        return types;
    }

    public Result run(CToolChain compiler, ObjectFileProvider objectFileProvider) throws IOException {
        Assert.checkNotNullParam("compiler", compiler);
        Assert.checkNotNullParam("objectFileProvider", objectFileProvider);

        StringBuilder b = new StringBuilder();
        // this is for size_t
        b.append("#include ").append("<stddef.h>");
        nl(b);
        for (Map.Entry<String, String> entry : defines.entrySet()) {
            nl(b);
            b.append("#define ").append(entry.getKey());
            String value = entry.getValue();
            if (! "1".equals(value)) {
                b.append(' ').append(value);
            }
        }
        nl(b);
        for (String include : includes) {
            nl(b);
            b.append("#include ").append(include);
        }
        nl(b);
        // size_t
        final UnaryOperator<StringBuilder> size_t = literal("size_t");
        decl(literal("unsigned long long"), "size_t_size", sizeof(size_t)).apply(b);
        nl(b);
        final UnaryOperator<StringBuilder> _Bool = literal("_Bool");
        for (int i = 0; i < types.size(); i++) {
            final Type typeObj = types.get(i);
            final UnaryOperator<StringBuilder> type;
            if (typeObj.getQualifier() == Qualifier.NONE) {
                type = literal(typeObj.getName());
            } else if (typeObj.getQualifier() == Qualifier.STRUCT) {
                type = struct(typeObj.getName(), UnaryOperator.identity());
            } else if (typeObj.getQualifier() == Qualifier.UNION) {
                type = union(typeObj.getName(), UnaryOperator.identity());
            } else {
                throw Assert.unreachableCode();
            }
            decl(_Bool, "t" + i + "_is_signed", isSigned(deref(zeroPtrTo(type)))).apply(b);
            nl(b);
            decl(_Bool, "t" + i + "_is_unsigned", isUnsigned(deref(zeroPtrTo(type)))).apply(b);
            nl(b);
            decl(_Bool, "t" + i + "_is_floating", isFloating(deref(zeroPtrTo(type)))).apply(b);
            nl(b);
            decl(size_t, "t" + i + "_overall_size", sizeof(type)).apply(b);
            decl(size_t, "t" + i + "_overall_align", alignof(type)).apply(b);
            nl(b);
            for (String memberName : typeObj.getMembers()) {
                decl(size_t, "t" + i + "_sizeof_" + memberName, sizeof(memberOf(type, memberName))).apply(b);
                decl(size_t, "t" + i + "_offsetof_" + memberName, offsetof(type, memberName)).apply(b);
                decl(_Bool, "t" + i + "_is_signed_" + memberName, isSigned(memberOf(type, memberName))).apply(b);
                decl(_Bool, "t" + i + "_is_unsigned_" + memberName, isUnsigned(memberOf(type, memberName))).apply(b);
                decl(_Bool, "t" + i + "_is_floating_" + memberName, isFloating(memberOf(type, memberName))).apply(b);
            }
        }
        // run the probe
        final Path path = Files.createTempFile("qcc-probe-", "." + objectFileProvider.getObjectType().objectSuffix());
        final CCompilerInvoker ib = compiler.newCompilerInvoker();
        ib.setOutputPath(path);
        ib.setSource(InputSource.from(b));
        ib.invoke();
        // read back the symbol info
        final Map<Type, Type.Info> typeInfos = new HashMap<>();
        final Map<Type, Map<String, Type.Info>> memberInfos = new HashMap<>();
        try (ObjectFile objectFile = objectFileProvider.openObjectFile(path)) {
            long size_t_size = objectFile.getSymbolValueAsLong("size_t_size");
            boolean isLong = size_t_size == 8;
            for (int i = 0; i < types.size(); i++) {
                final Type type = types.get(i);
                long overallSize = isLong ? objectFile.getSymbolValueAsLong("t" + i + "_overall_size") : objectFile.getSymbolValueAsInt("t" + i + "_overall_size");
                long overallAlign = isLong ? objectFile.getSymbolValueAsLong("t" + i + "_overall_align") : objectFile.getSymbolValueAsInt("t" + i + "_overall_align");
                boolean signed = objectFile.getSymbolValueAsByte("t" + i + "_is_signed") != 0;
                boolean unsigned = objectFile.getSymbolValueAsByte("t" + i + "_is_unsigned") != 0;
                boolean floating = objectFile.getSymbolValueAsByte("t" + i + "_is_floating") != 0;
                Type.Info info = new Type.Info(overallSize, overallAlign, 0, signed, unsigned, floating);
                Map<String, Type.Info> memberInfo = new HashMap<>();
                for (String memberName : type.getMembers()) {
                    final long memberSize = isLong ? objectFile.getSymbolValueAsLong("t" + i + "_sizeof_" + memberName) : objectFile.getSymbolValueAsInt("t" + i + "_sizeof_" + memberName);
                    final long memberOffset = isLong ? objectFile.getSymbolValueAsLong("t" + i + "_offsetof_" + memberName) : objectFile.getSymbolValueAsInt("t" + i + "_offsetof_" + memberName);
                    final boolean memberSigned = objectFile.getSymbolValueAsByte("t" + i + "_is_signed_" + memberName) != 0;
                    final boolean memberUnsigned = objectFile.getSymbolValueAsByte("t" + i + "_is_unsigned_" + memberName) != 0;
                    final boolean memberFloating = objectFile.getSymbolValueAsByte("t" + i + "_is_floating_" + memberName) != 0;
                    memberInfo.put(memberName, new Type.Info(memberSize, 0, memberOffset, memberSigned, memberUnsigned, memberFloating));
                }
                typeInfos.put(type, info);
                memberInfos.put(type, memberInfo);
            }
        }
        return new Result(typeInfos, memberInfos);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<String> includes;
        private Map<String, String> defines;
        private final List<Type> types = new ArrayList<>();

        Builder() {}

        public Builder addInclude(String include) {
            List<String> includes = this.includes;
            if (includes == null) {
                this.includes = includes = new ArrayList<>();
            }
            includes.add(Assert.checkNotNullParam("include", include));
            return this;
        }

        public Builder define(String name, String value) {
            Map<String, String> defines = this.defines;
            if (defines == null) {
                this.defines = defines = new HashMap<>();
            }
            defines.put(Assert.checkNotNullParam("name", name), Assert.checkNotNullParam("value", value));
            return this;
        }

        public Builder define(String name) {
            return define(name, "1");
        }

        public Builder addType(Type type) {
            types.add(Assert.checkNotNullParam("type", type));
            return this;
        }

        public CTypeProbe build() {
            return new CTypeProbe(this);
        }
    }

    public static final class Type {
        private final String name;
        private final Qualifier qualifier;
        private final List<String> members;
        private final int hashCode;

        Type(Builder builder) {
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

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private Qualifier qualifier = Qualifier.NONE;
            private List<String> members;

            Builder() {}

            public String getName() {
                return name;
            }

            public Builder setName(final String name) {
                this.name = Assert.checkNotNullParam("name", name);
                return this;
            }

            public Qualifier getQualifier() {
                return qualifier;
            }

            public Builder setQualifier(final Qualifier qualifier) {
                this.qualifier = Assert.checkNotNullParam("qualifier", qualifier);
                return this;
            }

            public Builder addMember(String name) {
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

    public static final class Result {
        private final Map<Type, Type.Info> typeInfos;
        private final Map<Type, Map<String, Type.Info>> memberInfos;

        Result(final Map<Type, Type.Info> typeInfos, final Map<Type, Map<String, Type.Info>> memberInfos) {
            this.typeInfos = typeInfos;
            this.memberInfos = memberInfos;
        }

        public Type.Info getInfo(Type type) throws NoSuchElementException {
            Type.Info info = typeInfos.get(type);
            if (info == null) {
                throw new NoSuchElementException();
            }
            return info;
        }

        public Type.Info getInfoOfMember(Type type, String memberName) throws NoSuchElementException {
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
    }
}
