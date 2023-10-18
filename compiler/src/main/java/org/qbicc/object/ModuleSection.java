package org.qbicc.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Value;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeUtil;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MemberElement;

/**
 * A section in a program module.
 * <p>
 * A section has a <em>simple name</em>, which is translated into a target-specific form. For example, some
 * targets might requires sections to begin with a leading period ({@code .}) character.
 * <p>
 * Sections are loaded into {@linkplain Segment segments}. Segments are predefined program areas that the
 * linker handles in a specific manner.
 */
public final class ModuleSection extends ProgramObject implements Comparable<ModuleSection> {
    final ProgramModule programModule;
    final Section section;
    // the list of section objects, unindexed (see ProgramModule#moduleObjects), in order
    // protected by programModule
    final List<SectionObject> objects = new ArrayList<>();
    // protected by programModule
    long offset;

    ModuleSection(final Section section, final ValueType valueType, final ProgramModule programModule) {
        super(section.getName(), valueType);
        this.section = section;
        this.programModule = programModule;
        if (section.isDataOnly()) {
            offset = 0;
        } else {
            offset = -1;
        }
    }

    public Data addData(MemberElement originalElement, String name, Value value) {
        return addData(originalElement, name, value, value.getType().getAlign());
    }

    public Data addData(MemberElement originalElement, String name, Value value, int align) {
        Data obj = new Data(originalElement,
            this, Assert.checkNotNullParam("name", name),
            Assert.checkNotNullParam("value", value).getType(),
            value
        );
        Map<String, ProgramObject> definedObjects = programModule.moduleObjects;
        synchronized (programModule) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                if (this.offset != -1) {
                    long offset = TypeUtil.alignUp(this.offset, Math.max(align, obj.getValueType().getAlign()));
                    obj.initOffset(offset);
                    this.offset = offset + obj.getSize();
                }
                objects.add(obj);
                definedObjects.put(name, obj);
            } else {
                if (existing instanceof DataDeclaration decl) {
                    if (! decl.getValueType().equals(obj.getValueType())) {
                        programModule.clash(originalElement, name);
                    } else {
                        obj.initDeclaration(decl);
                        if (offset != -1) {
                            // todo: max(obj align, type align)
                            obj.initOffset(offset);
                            this.offset = offset + obj.getSize();
                        }
                        objects.add(obj);
                        definedObjects.replace(name, decl, obj);
                    }
                } else {
                    twice(originalElement, name);
                }
            }
        }
        return obj;
    }

    /**
     * Get the current data offset of this module section.
     * Each new data object will increase this offset based on its size and alignment.
     *
     * @return the current offset
     * @throws IllegalArgumentException if the section is not data-only
     */
    public long getCurrentOffset() {
        long offset;
        synchronized (programModule) {
            offset = this.offset;
        }
        if (offset == -1) {
            throw new IllegalArgumentException("Not a data-only section");
        }
        return offset;
    }

    private void twice(MemberElement originalElement, final String name) {
        CompilationContext ctxt = programModule.getTypeDefinition().getContext().getCompilationContext();
        if (originalElement != null) {
            ctxt.error(originalElement, "Object '%s' defined twice", name);
        } else {
            ctxt.error("Synthetic object '%s' defined twice", name);
        }
    }

    public Function addFunction(ExecutableElement originalElement, String name, FunctionType type) {
        return addFunction(originalElement, name, type, originalElement == null ? SafePointBehavior.ENTER : originalElement.safePointBehavior());
    }

    public Function addFunction(ExecutableElement originalElement, String name, FunctionType type, SafePointBehavior safePointBehavior) {
        if (section.isDataOnly()) {
            throw dataOnlyException();
        }
        Function obj = new Function(originalElement,
            this, Assert.checkNotNullParam("name", name),
            Assert.checkNotNullParam("type", type),
            Function.getFunctionFlags(originalElement),
            safePointBehavior);
        Map<String, ProgramObject> definedObjects = programModule.moduleObjects;
        synchronized (programModule) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                objects.add(obj);
                definedObjects.put(name, obj);
                programModule.functions.add(obj);
            } else {
                if (existing instanceof FunctionDeclaration decl) {
                    if (! decl.getSymbolType().equals(obj.getSymbolType())) {
                        programModule.clash(originalElement, name);
                    } else {
                        obj.initDeclaration(decl);
                        objects.add(obj);
                        definedObjects.replace(name, existing, obj);
                        programModule.functions.add(obj);
                    }
                } else if (existing instanceof Function) {
                    twice(originalElement, name);
                } else {
                    programModule.clash(originalElement, name);
                }
            }
        }
        return obj;
    }

    public Iterable<SectionObject> contents() {
        synchronized (programModule) {
            return List.copyOf(objects);
        }
    }

    @Override
    public ProgramModule getProgramModule() {
        return programModule;
    }

    @Override
    public DataDeclaration getDeclaration() {
        return section.getSegmentStartDeclaration(programModule);
    }

    @Override
    public int compareTo(ModuleSection o) {
        return section.compareTo(o.section);
    }

    public Section getSection() {
        return section;
    }

    private static IllegalArgumentException dataOnlyException() {
        return new IllegalArgumentException("Cannot add function to data-only section");
    }
}
