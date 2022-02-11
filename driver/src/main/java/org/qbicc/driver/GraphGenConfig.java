package org.qbicc.driver;

import org.qbicc.type.definition.element.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

public class GraphGenConfig {
    public static final String ALL_METHODS = "all";
    public static final String ALL_PHASES = "all";

    private static final HashSet<String> ALL_METHODS_SET = new HashSet<>();

    private EnumMap<Phase, HashSet<String>> phaseToMethodsMap = new EnumMap<>(Phase.class);

    private boolean enabled;

    public void addMethodAndPhase(String method, String phaseString) {
        List<Phase> phaseList = new ArrayList<>();
        if (phaseString.equalsIgnoreCase(ALL_PHASES)) {
            phaseList.addAll(Arrays.asList(Phase.values()));
        } else {
            phaseList.add(Phase.getPhase(phaseString));
        }
        for (Phase p: phaseList) {
            if (method.equalsIgnoreCase(ALL_METHODS)) {
                phaseToMethodsMap.compute(p, (k, v) -> ALL_METHODS_SET);
            } else {
                // convert to internal representation
                //  eg convert java/lang/String.length() to java/lang.String.length()
                StringBuilder internalName = new StringBuilder(method);
                int endOfPackage = internalName.lastIndexOf("/");
                if (endOfPackage != -1) {
                    internalName = internalName.replace(endOfPackage, endOfPackage + 1, ".");
                }
                phaseToMethodsMap.computeIfAbsent(p, k -> new HashSet<>()).add(internalName.toString());
            }
        }
    }

    public GraphGenFilter getFilter() {
        return new GraphGenOptionsFilter();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    class GraphGenOptionsFilter implements GraphGenFilter {
        public boolean accept(Element element, Phase phase) {
            HashSet<String> methodSet = phaseToMethodsMap.get(phase);
            if (methodSet != null) {
                if (methodSet == ALL_METHODS_SET) {
                    return true;
                }
                return methodSet.contains(element.toString());
            }
            return false;
        }
    }
}
