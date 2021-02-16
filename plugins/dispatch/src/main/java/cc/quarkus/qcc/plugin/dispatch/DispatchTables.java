package cc.quarkus.qcc.plugin.dispatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.CompoundLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.Linkage;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.GlobalVariableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;
import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;

public class DispatchTables {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.dispatch");
    private static final Logger vtLog = Logger.getLogger("cc.quarkus.qcc.plugin.dispatch.vtables");

    private static final AttachmentKey<DispatchTables> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Map<ValidatedTypeDefinition, VTableInfo> vtables = new ConcurrentHashMap<>();
    private GlobalVariableElement vtablesGlobal;

    private DispatchTables(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static DispatchTables get(CompilationContext ctxt) {
        DispatchTables dt = ctxt.getAttachment(KEY);
        if (dt == null) {
            dt = new DispatchTables(ctxt);
            DispatchTables appearing = ctxt.putAttachmentIfAbsent(KEY, dt);
            if (appearing != null) {
                dt = appearing;
            }
        }
        return dt;
    }

    public VTableInfo getVTableInfo(ValidatedTypeDefinition cls) {
        return vtables.get(cls);
    }

    void buildFilteredVTable(ValidatedTypeDefinition cls) {
        log.debugf("Building VTable for %s", cls.getDescriptor());

        MethodElement[] inherited = cls.hasSuperClass() ? getVTableInfo(cls.getSuperClass()).getVtable() : MethodElement.NO_METHODS;
        ArrayList<MethodElement> vtableVector = new ArrayList<>(List.of(inherited));
        vtLog.debugf("\t inheriting %d methods", inherited.length);
        outer: for (int i=0; i<cls.getMethodCount(); i++) {
            MethodElement m = cls.getMethod(i);
            if (!m.isStatic() && ctxt.wasEnqueued(m)) {
                for (int j=0; j<inherited.length; j++) {
                    if (m.getName().equals(inherited[j].getName()) && m.getDescriptor().equals(inherited[j].getDescriptor())) {
                        vtLog.debugf("\tfound override for %s%s", m.getName(), m.getDescriptor().toString());
                        ctxt.registerEntryPoint(m);
                        vtableVector.set(j, m);
                        continue  outer;
                    }
                }
                vtLog.debugf("\tadded new method  %s%s", m.getName(), m.getDescriptor().toString());
                ctxt.registerEntryPoint(m);
                vtableVector.add(m);
            }
        }
        MethodElement[] vtable = vtableVector.toArray(MethodElement.NO_METHODS);

        String vtableName = "vtable-" + cls.getInternalName().replace('/', '.');
        TypeSystem ts = ctxt.getTypeSystem();
        CompoundType.Member[] functions = new CompoundType.Member[vtable.length];
        for (int i=0; i<vtable.length; i++) {
            FunctionType funType = ctxt.getExactFunction(vtable[i]).getType();
            functions[i] = ts.getCompoundTypeMember("m"+i, funType.getPointer(), i*ts.getPointerSize(), ts.getPointerAlignment());
        }
        CompoundType vtableType = ts.getCompoundType(CompoundType.Tag.STRUCT, vtableName, vtable.length * ts.getPointerSize(),
            ts.getPointerAlignment(), () -> List.of(functions));
        SymbolLiteral vtableSymbol = ctxt.getLiteralFactory().literalOfSymbol(vtableName, vtableType.getPointer());

        vtables.put(cls,new VTableInfo(vtable, vtableType, vtableSymbol));
    }

    void buildVTablesGlobal(DefinedTypeDefinition containingType) {
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName("qcc_vtables_array");
        // Invariant: typeIds are assigned from 1...N, where N is the number of reachable classes as computed by RTA.
        builder.setType(ctxt.getTypeSystem().getArrayType(ctxt.getTypeSystem().getVoidType().getPointer().getPointer(), vtables.size()+1));
        builder.setEnclosingType(containingType);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        vtablesGlobal = builder.build();
    }

    void emitVTable(ValidatedTypeDefinition cls) {
        VTableInfo info = getVTableInfo(cls);
        MethodElement[] vtable = info.getVtable();
        Section section = ctxt.getOrAddProgramModule(cls).getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
        for (int i=0; i<vtable.length; i++) {
            Function impl = ctxt.getExactFunction(vtable[i]);
            if (!vtable[i].getEnclosingType().validate().equals(cls)) {
                section.declareFunction(vtable[i], impl.getName(), impl.getType());
            }
            valueMap.put(info.getType().getMember(i), impl.getLiteral());
        }
        CompoundLiteral vtableLiteral = ctxt.getLiteralFactory().literalOf(info.getType(), valueMap);
        section.addData(null, info.getSymbol().getName(), vtableLiteral).setLinkage(Linkage.EXTERNAL);
    }

    void emitVTableTable(ValidatedTypeDefinition jlo) {
        ArrayType vtablesGlobalType = ((ArrayType)vtablesGlobal.getType(List.of()));
        Section section = ctxt.getOrAddProgramModule(jlo).getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        Literal[] vtableLiterals = new Literal[(int)vtablesGlobalType.getElementCount()];
        UndefinedLiteral undef =  ctxt.getLiteralFactory().literalOfUndefined();
        Arrays.fill(vtableLiterals,undef);
        vtableLiterals[0] = ctxt.getLiteralFactory().literalOfNull(); // typeId 0 is not assigned.
        for (Map.Entry<ValidatedTypeDefinition, VTableInfo> e: vtables.entrySet()) {
            if (!e.getKey().equals(jlo)) {
                section.declareData(null, e.getValue().getSymbol().getName(), e.getValue().getType());
            }
            int typeId = e.getKey().getTypeId();
            Assert.assertTrue(vtableLiterals[typeId].equals(undef));
            vtableLiterals[e.getKey().getTypeId()] = ctxt.getLiteralFactory().bitcastLiteral(e.getValue().getSymbol(), (WordType)vtablesGlobalType.getElementType());
        }
        ArrayLiteral masterValue = ctxt.getLiteralFactory().literalOf(vtablesGlobalType, List.of(vtableLiterals));
        section.addData(null, vtablesGlobal.getName(), masterValue);
    }

    public GlobalVariableElement getVTablesGlobal() { return this.vtablesGlobal; }

    public int getVTableIndex(MethodElement target) {
        ValidatedTypeDefinition definingType = target.getEnclosingType().validate();
        VTableInfo info = getVTableInfo(definingType);
        if (info != null) {
            MethodElement[] vtable = info.getVtable();
            for (int i = 0; i < vtable.length; i++) {
                if (target.getName().equals(vtable[i].getName()) && target.getDescriptor().equals(vtable[i].getDescriptor())) {
                    return i;
                }
            }
        }
        ctxt.error("No vtable entry found for "+target);
        return 0;
    }

    public static final class VTableInfo {
        private final MethodElement[] vtable;
        private final CompoundType type;
        private final SymbolLiteral symbol;

        VTableInfo(MethodElement[] vtable, CompoundType type, SymbolLiteral symbol) {
            this.vtable = vtable;
            this.type = type;
            this.symbol = symbol;
        }

        public MethodElement[] getVtable() { return vtable; }
        public SymbolLiteral getSymbol() { return symbol; }
        public CompoundType getType() { return  type; }
    }
}
