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
import cc.quarkus.qcc.type.ValueType;
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
    final Map<String, DataDeclaration> declaredData = new ConcurrentHashMap<>();
    final List<ProgramObject> contents = Collections.synchronizedList(new ArrayList<>());

    Section(final String name, final SymbolLiteral literal, final ProgramModule programModule) {
        super(name, literal);
        this.programModule = programModule;
    }

    public Data addData(MemberElement originalElement, String name, Value value) {
        Data obj = new Data(
            originalElement, Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, Assert.checkNotNullParam("value", value).getType()),
            value
        );
        if (definedObjects.add(name)) {
            return add(obj);
        } else {
            twice(originalElement, name);
            return obj;
        }
    }

    private static void twice(final MemberElement originalElement, final String name) {
        originalElement.getEnclosingType().getContext().getCompilationContext().error(originalElement, "Object '%s' defined twice", name);
    }

    public Function addFunction(ExecutableElement originalElement, String name, FunctionType type) {
        Function obj = new Function(
            originalElement, Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, type)
        );
        if (definedObjects.add(name)) {
            return add(obj);
        } else {
            twice(originalElement, name);
            return obj;
        }
    }

    public FunctionDeclaration declareFunction(ExecutableElement originalElement, String name, FunctionType type) {
        Assert.checkNotNullParam("name", name);
        FunctionDeclaration decl = declaredFunctions.get(name);
        if (decl == null) {
            decl = new FunctionDeclaration(
                originalElement, name,
                programModule.literalFactory.literalOfSymbol(name, type)
            );
            FunctionDeclaration appearing = declaredFunctions.putIfAbsent(name, decl);
            if (appearing == null) {
                return add(decl);
            }
            decl = appearing;
        }
        if (! type.equals(decl.getType())) {
            clash(originalElement, name);
        }
        return decl;
    }

    public DataDeclaration declareData(MemberElement originalElement, String name, ValueType type) {
        Assert.checkNotNullParam("name", name);
        DataDeclaration decl = declaredData.get(name);
        if (decl == null) {
            decl = new DataDeclaration(originalElement, name,
                programModule.literalFactory.literalOfSymbol(name, type)
            );
            DataDeclaration appearing = declaredData.putIfAbsent(name, decl);
            if (appearing == null) {
                return add(decl);
            }
            decl = appearing;
        }
        if (! type.equals(decl.getType())) {
            clash(originalElement, name);
        }
        return decl;
    }

    private static void clash(final MemberElement originalElement, final String name) {
        originalElement.getEnclosingType().getContext().getCompilationContext().error(originalElement, "Object '%s' redeclared with different type", name);
    }


    private <T extends ProgramObject> T add(T item) {
        contents.add(item);
        return item;
    }

    public Iterable<ProgramObject> contents() {
        return List.of(contents.toArray(ProgramObject[]::new));
    }
}
