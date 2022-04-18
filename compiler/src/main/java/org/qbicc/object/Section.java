package org.qbicc.object;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.TypeSystem;

/**
 * A program section. Program modules define objects into sections.
 */
public final class Section implements Comparable<Section> {
    private final int index;
    private final String name;
    private final Segment segment;
    private final boolean dataOnly;

    /**
     * Construct a new instance.
     *
     * @param index the section index
     * @param name the section name (must not be {@code null})
     * @param segment the section segments (must not be {@code null})
     * @param attributes the section attributes, if any
     */
    public Section(int index, String name, Segment segment, Attribute... attributes) {
        this.index = index;
        this.name = name;
        this.segment = segment;
        boolean dataOnly = false;
        for (Attribute attribute : attributes) {
            if (attribute == Flag.DATA_ONLY) {
                dataOnly = true;
                continue;
            }
        }
        this.dataOnly = dataOnly;
    }

    @Override
    public int compareTo(Section o) {
        int res = segment.compareTo(o.segment);
        if (res == 0) res = Integer.compare(index, o.index);
        if (res == 0) res = name.compareTo(o.name);
        return res;
    }

    /**
     * Get the section index.
     * The index is used for ordering the section within the segment.
     * Sections with lower indices will be placed earlier (lower) in memory than sections with higher indices.
     *
     * @return the section index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the section name.
     * Note that section names are typically transformed in some target-specific manner.
     * Therefore, the name that appears in the output assembly might differ.
     *
     * @return the section name (not {@code null})
     */
    public String getName() {
        return name;
    }

    /**
     * Get the segment.
     * The section will be loaded into the given segment.
     *
     * @return the segment (not {@code null})
     */
    public Segment getSegment() {
        return segment;
    }

    /**
     * Determine whether this is the implicit section.
     *
     * @return {@code true} if this is the implicit section, or {@code false} otherwise
     */
    public boolean isImplicit() {
        return getName().equals(CompilationContext.IMPLICIT_SECTION_NAME);
    }

    /**
     * Determine whether this is a data-only section.
     *
     * @return {@code true} if this is a data-only section, or {@code false} if it is a mixed code and data section
     */
    public boolean isDataOnly() {
        return dataOnly;
    }

    /**
     * Get a data declaration whose value is the start address of the segment.
     * If an existing declaration exists, it is returned.
     * The type of the declaration is pointer-to-{@code void} so it must be cast before it can be used.
     *
     * @param programModule the program module into which the declaration should be created
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration getSegmentStartDeclaration(ProgramModule programModule) {
        CompilationContext ctxt = programModule.getTypeDefinition().getContext().getCompilationContext();
        TypeSystem ts = ctxt.getTypeSystem();
        return programModule.declareData(null, ctxt.getPlatform().formatStartOfSectionSymbolName(segment.toString(), name), ts.getVoidType().getPointer());
    }

    /**
     * Get a data declaration whose value is the end address of the segment.
     * If an existing declaration exists, it is returned.
     * The type of the declaration is pointer-to-{@code void} so it must be cast before it can be used.
     *
     * @param programModule the program module into which the declaration should be created
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration getSegmentEndDeclaration(ProgramModule programModule) {
        CompilationContext ctxt = programModule.getTypeDefinition().getContext().getCompilationContext();
        TypeSystem ts = ctxt.getTypeSystem();
        return programModule.declareData(null, ctxt.getPlatform().formatEndOfSectionSymbolName(segment.toString(), name), ts.getVoidType().getPointer());
    }

    /**
     * An attribute of a section.
     */
    public abstract static class Attribute {
        Attribute() {}
    }

    /**
     * Boolean-typed section attributes.
     */
    public static final class Flag extends Attribute {
        private final String name;

        private Flag(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Flag flag && equals(flag);
        }

        public boolean equals(Flag other) {
            return this == other;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * A flag indicating that this section may <em>only</em> contain data.
         * Objects in data-only sections are fixed in size and thus have knowable offsets.
         */
        public static final Flag DATA_ONLY = new Flag("DATA_ONLY");
    }
}
