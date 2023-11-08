package org.qbicc.plugin.native_;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The result of probing a single platform type.
 */
public record YamlProbeInfo(Kind kind, Tag tag, String moduleName, int size, int align, Map<String, YamlMemberInfo> members) {

    public YamlProbeInfo {
        kind = Objects.requireNonNullElse(kind, Kind.incomplete);
        tag = Objects.requireNonNullElse(tag, Tag.none);
        moduleName = Objects.requireNonNullElse(moduleName, "").intern();
        members = Map.copyOf(Objects.requireNonNullElse(members, Map.of()));
    }

    public YamlProbeInfo(Kind kind, int size, int align) {
        this(kind, Tag.none, "", size, align, Map.of());
    }

    public enum Kind {
        @JsonProperty("boolean")
        boolean_,
        incomplete,
        struct,
        union,
        signed_integer,
        unsigned_integer,
        @JsonProperty("float")
        float_,
        pointer,
        @JsonProperty("void")
        void_,
        ;
    }

    public enum Tag {
        none,
        struct,
        union,
        ;
    }
}
