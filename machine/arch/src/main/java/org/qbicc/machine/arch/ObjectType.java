package org.qbicc.machine.arch;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ObjectType extends PlatformComponent {

    public static final ObjectType UNKNOWN = new ObjectType("unknown", "o");
    public static final ObjectType ELF = new ObjectType("elf", "o");
    public static final ObjectType MACH_O = new ObjectType("macho", "o");
    public static final ObjectType COFF = new ObjectType("coff", "obj");

    private final String objectSuffix;

    ObjectType(final String name, final String objectSuffix) {
        super(name);
        this.objectSuffix = objectSuffix;
    }

    public String objectSuffix() {
        return objectSuffix;
    }

    private static final Map<String, ObjectType> index = Indexer.index(ObjectType.class);

    public static ObjectType forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }

    /**
     * Format a section name according to the rules of this object file format.
     *
     * @param segmentName the segment name (must not be {@code null})
     * @param simpleName the simple name (must not be {@code null})
     * @return the object-specific section name (not {@code null})
     */
    public String formatSectionName(final String segmentName, final String simpleName) {
        if (this == MACH_O) {
            return formatSegmentName(segmentName) + ",__" + simpleName;
        } else {
            return "." + simpleName;
        }
    }

    /**
     * Format a segment name according to the rules of this object file format.
     *
     * @param name the segment name (must not be {@code null})
     * @return the object-specific segment name (not {@code null})
     */
    public String formatSegmentName(final String name) {
        if (this == MACH_O) {
            return "__" + name.toUpperCase(Locale.ROOT);
        } else {
            return name + "-segment";
        }
    }

    /**
     * Format the start-of-segment name according to the rules of this object file format.
     *
     * @param segmentName the segment name (must not be {@code null})
     * @param simpleName  the section's simple name (must not be {@code null})
     * @return the object-specific symbol (not {@code null})
     */
    public String formatStartOfSectionSymbolName(final String segmentName, final String simpleName) {
        // todo: COFF: https://stackoverflow.com/questions/3808053/how-to-get-a-pointer-to-a-binary-section-in-msvc?noredirect=1&lq=1
        if (this == MACH_O) {
            return "section$start$" + formatSegmentName(segmentName) + "$__" + simpleName;
        } else {
            return "__start_" + simpleName;
        }
    }

    /**
     * Format the end-of-segment name according to the rules of this object file format.
     *
     * @param segmentName the segment name (must not be {@code null})
     * @param simpleName  the section's simple name (must not be {@code null})
     * @return the object-specific symbol (not {@code null})
     */
    public String formatEndOfSectionSymbolName(final String segmentName, final String simpleName) {
        if (this == MACH_O) {
            return "section$end$" + formatSegmentName(segmentName) + "$__" + simpleName;
        } else {
            return "__stop_" + simpleName;
        }
    }
}
