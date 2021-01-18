package cc.quarkus.qcc.plugin.dispatch;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DispatchTables {
    private static final AttachmentKey<DispatchTables> KEY = new AttachmentKey<>();
    private static final boolean DEBUG_VTABLES = false;

    private final CompilationContext ctxt;
    private final Map<ValidatedTypeDefinition, MethodElement[]> vtables = new ConcurrentHashMap<>();

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

    public MethodElement[] getVTable(ValidatedTypeDefinition cls) {
        return vtables.getOrDefault(cls, MethodElement.NO_METHODS);
    }

    void buildFilteredVTable(ValidatedTypeDefinition cls) {
        if (DEBUG_VTABLES) ctxt.debug("Building VTable for "+cls.getDescriptor());

        MethodElement[] inherited = cls.hasSuperClass() ? getVTable(cls.getSuperClass()) : MethodElement.NO_METHODS;
        ArrayList<MethodElement> vtableVector = new ArrayList<>(List.of(inherited));
        if (DEBUG_VTABLES) ctxt.debug("\t inheriting %d methods", inherited.length);
        outer: for (int i=0; i<cls.getMethodCount(); i++) {
            MethodElement m = cls.getMethod(i);
            if (!m.isStatic() && ctxt.wasEnqueued(m)) {
                for (int j=0; j<inherited.length; j++) {
                    if (m.getName().equals(inherited[j].getName()) && m.getDescriptor().equals(inherited[j].getDescriptor())) {
                        if (DEBUG_VTABLES) ctxt.debug("\tfound override for %s%s", m.getName(), m.getDescriptor().toString());
                        vtableVector.set(j, m);
                        continue  outer;
                    }
                }
                if (DEBUG_VTABLES) ctxt.debug("\tadded new method  %s%s", m.getName(), m.getDescriptor().toString());
                vtableVector.add(m);
            }
        }

        vtables.put(cls, vtableVector.toArray(new MethodElement[vtableVector.size()]));
    }
}
