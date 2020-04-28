package cc.quarkus.qcc.graph.build;

import java.util.HashSet;
import java.util.Set;

import cc.quarkus.qcc.type.TypeDefinition;

public class CatchMatcher {

    public CatchMatcher() {
    }

    public void addIncludeType(TypeDefinition includeType) {
        this.includeTypes.add(includeType);
    }

    public void addExcludeType(TypeDefinition excludeType) {
        for (TypeDefinition includeType : this.includeTypes) {
            if ( includeType.isAssignableFrom(excludeType) ) {
                this.excludeTypes.add(excludeType);
            }
        }
    }

    public Set<TypeDefinition> getIncludeTypes() {
        return this.includeTypes;
    }

    public Set<TypeDefinition> getExcludeTypes() {
        return this.excludeTypes;
    }

    public boolean isMatchAll() {
        return this.includeTypes.isEmpty() && this.excludeTypes.isEmpty();
    }

    @Override
    public String toString() {
        return this.includeTypes + ( this.excludeTypes.isEmpty() ? "" : " & ~" + this.excludeTypes);
    }

    public boolean matches(TypeDefinition typeDefinition) {
        if ( isMatchAll() ) {
            return true;
        }

        if ( this.includeTypes.stream().noneMatch(e -> e.isAssignableFrom(typeDefinition)) ) {
            return false;
        }

        return this.excludeTypes.stream().anyMatch(e->e.isAssignableFrom(typeDefinition));
    }

    private Set<TypeDefinition> includeTypes = new HashSet<>();
    private Set<TypeDefinition> excludeTypes = new HashSet<>();

}
