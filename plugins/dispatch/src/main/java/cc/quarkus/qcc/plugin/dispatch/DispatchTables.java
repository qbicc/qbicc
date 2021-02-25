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
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.Linkage;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
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
    private final Map<ValidatedTypeDefinition, ITableInfo> itables = new ConcurrentHashMap<>();
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

    public ITableInfo getITableInfo(ValidatedTypeDefinition cls) { return itables.get(cls); }

    void buildFilteredVTable(ValidatedTypeDefinition cls) {
        vtLog.debugf("Building VTable for %s", cls.getDescriptor());

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
                vtLog.debugf("\tadded new method %s%s", m.getName(), m.getDescriptor().toString());
                ctxt.registerEntryPoint(m);
                vtableVector.add(m);
            }
        }
        MethodElement[] vtable = vtableVector.toArray(MethodElement.NO_METHODS);

        String vtableName = "vtable-" + cls.getInternalName().replace('/', '.');
        TypeSystem ts = ctxt.getTypeSystem();
        CompoundType.Member[] functions = new CompoundType.Member[vtable.length];
        for (int i=0; i<vtable.length; i++) {
            FunctionType funType = ctxt.getFunctionTypeForElement(vtable[i]);
            functions[i] = ts.getCompoundTypeMember("m"+i, funType.getPointer(), i*ts.getPointerSize(), ts.getPointerAlignment());
        }
        CompoundType vtableType = ts.getCompoundType(CompoundType.Tag.STRUCT, vtableName, vtable.length * ts.getPointerSize(),
            ts.getPointerAlignment(), () -> List.of(functions));
        SymbolLiteral vtableSymbol = ctxt.getLiteralFactory().literalOfSymbol(vtableName, vtableType.getPointer());

        vtables.put(cls,new VTableInfo(vtable, vtableType, vtableSymbol));
    }

    void buildFilteredITableForInterface(ValidatedTypeDefinition cls) {
        if (itables.containsKey(cls)) {
            return; // already built; possible because we aren't doing a coordinated topological traversal of the interface hierarchy.
        }

        // First, ensure my ancestors have already been computed computed
        for (ValidatedTypeDefinition si : cls.getInterfaces()) {
            if (!itables.containsKey(si)) {
                buildFilteredITableForInterface(si);
            }
        }

        // Now we can really start...
        vtLog.debugf("Building ITable for %s", cls.getDescriptor());

        // Accumulate all unique selectors from super-interfaces
        ArrayList<MethodElement> itableVector = new ArrayList<>();
        for (ValidatedTypeDefinition si : cls.getInterfaces()) {
            ITableInfo siInfo = getITableInfo(si);
            outer:
            for (MethodElement m : siInfo.getItable()) {
                for (MethodElement already : itableVector) {
                    if (already.getName().equals(m.getName()) && already.getDescriptor().equals(m.getDescriptor())) {
                        continue outer;
                    }
                }
                vtLog.debugf("\tadded inherited selector %s%s from %s", m.getName(), m.getDescriptor().toString(), si.getDescriptor().getClassName());
                itableVector.add(m);
            }
        }

        // Add any additional unique selectors declared by cls
        outer:
        for (int i = 0; i < cls.getMethodCount(); i++) {
            MethodElement m = cls.getMethod(i);
            if (!m.isStatic() && ctxt.wasEnqueued(m)) {
                for (MethodElement already : itableVector) {
                    if (already.getName().equals(m.getName()) && already.getDescriptor().equals(m.getDescriptor())) {
                        continue outer;
                    }
                }
                vtLog.debugf("\tadded new declared selector %s%s", m.getName(), m.getDescriptor().toString());
                itableVector.add(m);
            }
        }

        // Build the CompoundType for the ITable using the (arbitrary) order of selectors in itableVector
        MethodElement[] itable = itableVector.toArray(MethodElement.NO_METHODS);
        String itableName = "itable-" + cls.getInternalName().replace('/', '.');
        TypeSystem ts = ctxt.getTypeSystem();
        CompoundType.Member[] functions = new CompoundType.Member[itable.length];
        for (int i=0; i<itable.length; i++) {
            FunctionType funType = ctxt.getFunctionTypeForElement(itable[i]);
            functions[i] = ts.getCompoundTypeMember("m"+i, funType.getPointer(), i*ts.getPointerSize(), ts.getPointerAlignment());
        }
        CompoundType itableType = ts.getCompoundType(CompoundType.Tag.STRUCT, itableName, itable.length * ts.getPointerSize(),
            ts.getPointerAlignment(), () -> List.of(functions));

        // Define the GlobalVariable that will hold the itables[] for this interface.
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName("qcc_itables_array_"+itableType.getName());
        // Yet another table indexed by typeId (like the VTableGlobal) that will only contain entries for instantiated classes.
        // Use the VTableGlobal to set the size to avoid replicating that logic...
        builder.setType(ctxt.getTypeSystem().getArrayType(itableType.getPointer(), ((ArrayType)vtablesGlobal.getType(List.of())).getElementCount()));
        builder.setEnclosingType(cls);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        GlobalVariableElement itablesGlobal = builder.build();

        itables.put(cls, new ITableInfo(itable, itableType, cls, itablesGlobal));
    }

    void buildVTablesGlobal(DefinedTypeDefinition containingType) {
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName("qcc_vtables_array");
        // Invariant: typeIds are assigned from 1...N, where N is the number of reachable classes as computed by RTA 
        // plus 18 for 8 primitive types, void, 8 primitive arrays and reference array.
        builder.setType(ctxt.getTypeSystem().getArrayType(ctxt.getTypeSystem().getVoidType().getPointer().getPointer(), vtables.size()+19));  //TODO: communicate this +19 better
        builder.setEnclosingType(containingType);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        vtablesGlobal = builder.build();
    }

    void emitVTable(ValidatedTypeDefinition cls) {
        if (!cls.isAbstract()) {
            VTableInfo info = getVTableInfo(cls);
            MethodElement[] vtable = info.getVtable();
            Section section = ctxt.getImplicitSection(cls);
            HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
            for (int i = 0; i < vtable.length; i++) {
                FunctionType funType = ctxt.getFunctionTypeForElement(vtable[i]);
                if (vtable[i].isAbstract()) {
                    // TODO: In a correct program, this null should never be used. However, we'd get better
                    //       debugability if we initialized it to an "AbstractMethodInvoked" error stub
                    valueMap.put(info.getType().getMember(i), ctxt.getLiteralFactory().zeroInitializerLiteralOfType(funType));
                } else {
                    Function impl = ctxt.getExactFunction(vtable[i]);
                    if (!vtable[i].getEnclosingType().validate().equals(cls)) {
                        section.declareFunction(vtable[i], impl.getName(), funType);
                    }
                    valueMap.put(info.getType().getMember(i), impl.getLiteral());
                }
            }
            CompoundLiteral vtableLiteral = ctxt.getLiteralFactory().literalOf(info.getType(), valueMap);
            section.addData(null, info.getSymbol().getName(), vtableLiteral).setLinkage(Linkage.EXTERNAL);
        }
    }

    void emitVTableTable(ValidatedTypeDefinition jlo) {
        ArrayType vtablesGlobalType = ((ArrayType)vtablesGlobal.getType(List.of()));
        Section section = ctxt.getImplicitSection(jlo);
        Literal[] vtableLiterals = new Literal[(int)vtablesGlobalType.getElementCount()];
        Literal zeroLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(vtablesGlobalType.getElementType());
        Arrays.fill(vtableLiterals, zeroLiteral);
        for (Map.Entry<ValidatedTypeDefinition, VTableInfo> e: vtables.entrySet()) {
            ValidatedTypeDefinition cls = e.getKey();
            if (!cls.isAbstract()) {
                if (!cls.equals(jlo)) {
                    section.declareData(null, e.getValue().getSymbol().getName(), e.getValue().getType());
                }
                int typeId = cls.getTypeId();
                Assert.assertTrue(vtableLiterals[typeId].equals(zeroLiteral));
                vtableLiterals[typeId] = ctxt.getLiteralFactory().bitcastLiteral(e.getValue().getSymbol(), (WordType) vtablesGlobalType.getElementType());
            }
        }
        ArrayLiteral vtablesGlobalValue = ctxt.getLiteralFactory().literalOf(vtablesGlobalType, List.of(vtableLiterals));
        section.addData(null, vtablesGlobal.getName(), vtablesGlobalValue);
    }

    public GlobalVariableElement getVTablesGlobal() { return this.vtablesGlobal; }

    public void emitInterfaceTables(RTAInfo rtaInfo) {
        for (Map.Entry<ValidatedTypeDefinition, ITableInfo> entry: itables.entrySet()) {
            ValidatedTypeDefinition currentInterface = entry.getKey();
            Section iSection = ctxt.getImplicitSection(currentInterface);
            ITableInfo itableInfo= entry.getValue();
            MethodElement[] itable = itableInfo.getItable();
            if (itable.length == 0) {
                continue; // If there are no invokable methods then this whole family of itables will never be referenced.
            }
            ArrayType rootType = (ArrayType)itableInfo.getGlobal().getType(List.of());
            Literal[] rootTable = new Literal[(int)rootType.getElementCount()];
            Literal zeroLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(rootType.getElementType());
            Arrays.fill(rootTable, zeroLiteral);
            rtaInfo.visitLiveImplementors(currentInterface, cls -> {
                if (!cls.isAbstract() && !cls.isInterface()) {
                    // Build the itable for an instantiable class
                    HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
                    Section cSection = ctxt.getImplicitSection(cls);
                    for (int i = 0; i < itable.length; i++) {
                        MethodElement methImpl = cls.resolveMethodElementVirtual(itable[i].getName(), itable[i].getDescriptor());
                        FunctionType funType = ctxt.getFunctionTypeForElement(itable[i]);
                        FunctionType implType = ctxt.getFunctionTypeForElement(methImpl);
                        if (methImpl == null || methImpl.isAbstract()) {
                            // TODO: In a correct program, this null should never be used. However, we'd get better
                            //       debugability if we initialized it to an "AbstractMethodInvoked" error stub
                            valueMap.put(itableInfo.getType().getMember(i), ctxt.getLiteralFactory().zeroInitializerLiteralOfType(funType));
                        } else {
                            Function impl = ctxt.getExactFunction(methImpl);
                            if (!methImpl.getEnclosingType().validate().equals(cls)) {
                                cSection.declareFunction(methImpl, impl.getName(), implType);
                            }
                            valueMap.put(itableInfo.getType().getMember(i), impl.getLiteral());
                        }
                    }

                    // Emit itable and refer to it in rootTable
                    String tableName = "qcc_itable_impl_"+cls.getInternalName().replace('/', '.')+"_for_"+itableInfo.getGlobal().getName();
                    CompoundLiteral itableLiteral = ctxt.getLiteralFactory().literalOf(itableInfo.getType(), valueMap);
                    cSection.addData(null, tableName, itableLiteral).setLinkage(Linkage.EXTERNAL);
                    iSection.declareData(null, tableName, itableInfo.getType());
                    rootTable[cls.getTypeId()] = ctxt.getLiteralFactory().literalOfSymbol(tableName, itableInfo.getType());
                }
            });

            // Finally emit the iTable[] for the interface
            iSection.addData(null, itableInfo.getGlobal().getName(), ctxt.getLiteralFactory().literalOf(rootType, List.of(rootTable)));
        }
    }


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

    public int getITableIndex(MethodElement target) {
        ValidatedTypeDefinition definingType = target.getEnclosingType().validate();
        ITableInfo info = getITableInfo(definingType);
        if (info != null) {
            MethodElement[] itable = info.getItable();
            for (int i = 0; i < itable.length; i++) {
                if (target.getName().equals(itable[i].getName()) && target.getDescriptor().equals(itable[i].getDescriptor())) {
                    return i;
                }
            }
        }
        ctxt.error("No itable entry found for "+target);
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

    public static final class ITableInfo {
        private final ValidatedTypeDefinition myInterface;
        private final MethodElement[] itable;
        private final CompoundType type;
        private final GlobalVariableElement global;

        ITableInfo(MethodElement[] itable, CompoundType type, ValidatedTypeDefinition myInterface, GlobalVariableElement global) {
            this.myInterface = myInterface;
            this.itable = itable;
            this.type = type;
            this.global = global;
        }

        public ValidatedTypeDefinition getInterface() { return myInterface; }
        public MethodElement[] getItable() { return itable; }
        public CompoundType getType() { return type; }
        public GlobalVariableElement getGlobal() { return global; }
    }
}
