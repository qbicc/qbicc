package org.qbicc.plugin.wasm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.qbicc.context.CompilationContext;
import org.qbicc.machine.file.wasm.model.DefinedFunc;
import org.qbicc.machine.file.wasm.model.Module;
import org.qbicc.object.Data;
import org.qbicc.object.Function;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.object.SectionObject;

/**
 * The generator for the WASM module.
 */
public final class WasmGenerator implements Consumer<CompilationContext> {
    private final Module module;

    public WasmGenerator(String moduleName) {
        this.module = new Module(moduleName);
    }

    @Override
    public void accept(CompilationContext ctxt) {
        List<ProgramModule> allProgramModules = ctxt.getAllProgramModules();
        // place *all* data!
        // data offset within section
        MutableObjectIntMap<Data> dataOffsets = ObjectIntMaps.mutable.empty();
        // section size
        MutableObjectIntMap<Section> sectionSizes = ObjectIntMaps.mutable.empty();
        for (ProgramModule programModule : allProgramModules) {
            for (ModuleSection section : programModule.sections()) {
                for (SectionObject item : section.contents()) {
                    if (item instanceof Data d) {
                        // todo: v/itables are special and do not get placed
                        int dataOffset = sectionSizes.getIfAbsent(section.getSection(), 0);
                        dataOffsets.put(d, dataOffset);
                        sectionSizes.put(section.getSection(), Math.addExact(dataOffset, Math.toIntExact(d.getSize())));
                    }
                }
            }
        }
        // section absolute offset from start of data
        MutableObjectIntMap<Section> sectionOffsets = ObjectIntMaps.mutable.empty();
        int off = 0;
        // TODO: place sections in order so that heap stuff is later
        for (Section section : sectionSizes.keySet()) {
            sectionOffsets.put(section, off);
            off += sectionSizes.get(section);
        }
        Iterator<ProgramModule> iterator = allProgramModules.iterator();
        // TODO: pass the data locations in to the note visitor
        ctxt.runParallelTask(ctxt_ -> {
            for (;;) {
                ProgramModule programModule;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    programModule = iterator.next();
                }
                List<DefinedFunc> definedFuncs = new ArrayList<>();
                for (ModuleSection section : programModule.sections()) {
                    for (SectionObject item : section.contents()) {
                        if (item instanceof Function fn) {
                            definedFuncs.add(new WasmNodeVisitor(ctxt_, fn.getOriginalElement()).run());
                        }
                    }
                }
                synchronized (module) {
                    for (DefinedFunc definedFunc : definedFuncs) {
                        module.define(definedFunc);
                    }
                }
            }
        });
    }
}
