package cc.quarkus.qcc.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MemberElement;
import io.smallrye.common.constraint.Assert;

/**
 * A section in a program.
 */
public final class Section extends ProgramObject {
    final ProgramModule programModule;
    final Set<String> definedObjects = ConcurrentHashMap.newKeySet();
    final Map<String, FunctionDeclaration> declaredFunctions = new ConcurrentHashMap<>();
    final List<ProgramObject> contents = Collections.synchronizedList(new ArrayList<>());

    Section(final String name, final SymbolLiteral literal, final ProgramModule programModule) {
        super(name, literal);
        this.programModule = programModule;
    }

    public Data addData(MemberElement originalElement, String name, Value value) {
        if (definedObjects.add(name)) {
            return add(new Data(
                originalElement, Assert.checkNotNullParam("name", name),
                programModule.literalFactory.literalOfSymbol(name, Assert.checkNotNullParam("value", value).getType()),
                value
            ));
        } else {
            // todo: make this a compile error
            throw new IllegalStateException("Object " + name + " defined twice");
        }
    }

    public Function addFunction(ExecutableElement originalElement, String name, FunctionType type) {
        if (definedObjects.add(name)) {
            return add(new Function(
                originalElement, Assert.checkNotNullParam("name", name),
                programModule.literalFactory.literalOfSymbol(name, type)
            ));
        } else {
            // todo: make this a compile error
            throw new IllegalStateException("Object " + name + " defined twice");
        }
    }

    public FunctionDeclaration declareFunction(ExecutableElement originalElement, String name, FunctionType type) {
        FunctionDeclaration decl = declaredFunctions.get(name);
        if (decl == null) {
            decl = new FunctionDeclaration(
                originalElement, Assert.checkNotNullParam("name", name),
                programModule.literalFactory.literalOfSymbol(name, type)
            );
            FunctionDeclaration appearing = declaredFunctions.putIfAbsent(name, decl);
            if (appearing == null) {
                return add(decl);
            }
            decl = appearing;
        }
        if (! type.equals(decl.getType())) {
            // todo: make this a compile error
            throw new IllegalStateException("Function " + name + " redeclared with different type");
        }
        return decl;
    }

    private <T extends ProgramObject> T add(T item) {
        contents.add(item);
        return item;
    }

    public Iterable<ProgramObject> contents() {
        return List.of(contents.toArray(ProgramObject[]::new));
    }
}
