package cc.quarkus.qcc.plugin.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import org.jboss.logging.Logger;

public class DispatchTables {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.dispatch");
    private static final Logger vtLog = Logger.getLogger("cc.quarkus.qcc.plugin.dispatch.vtables");

    private static final AttachmentKey<DispatchTables> KEY = new AttachmentKey<>();

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
        log.debugf("Building VTable for %s", cls.getDescriptor());

        MethodElement[] inherited = cls.hasSuperClass() ? getVTable(cls.getSuperClass()) : MethodElement.NO_METHODS;
        ArrayList<MethodElement> vtableVector = new ArrayList<>(List.of(inherited));
        vtLog.debugf("\t inheriting %d methods", inherited.length);
        outer: for (int i=0; i<cls.getMethodCount(); i++) {
            MethodElement m = cls.getMethod(i);
            if (!m.isStatic() && ctxt.wasEnqueued(m)) {
                for (int j=0; j<inherited.length; j++) {
                    if (m.getName().equals(inherited[j].getName()) && m.getDescriptor().equals(inherited[j].getDescriptor())) {
                        vtLog.debugf("\tfound override for %s%s", m.getName(), m.getDescriptor().toString());
                        vtableVector.set(j, m);
                        continue  outer;
                    }
                }
                vtLog.debugf("\tadded new method  %s%s", m.getName(), m.getDescriptor().toString());
                vtableVector.add(m);
            }
        }

        vtables.put(cls, vtableVector.toArray(MethodElement.NO_METHODS));
    }
}
