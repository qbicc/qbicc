package cc.quarkus.qcc.object;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    final Map<String, ProgramObject> definedObjects = Collections.synchronizedMap(new LinkedHashMap<>());

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
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                definedObjects.put(name, obj);
            } else {
                if (existing instanceof DataDeclaration) {
                    if (! existing.getType().equals(obj.getType())) {
                        clash(originalElement, name);
                    } else {
                        definedObjects.replace(name, existing, obj);
                    }
                } else {
                    twice(originalElement, name);
                }
            }
        }
        return obj;
    }

    private void twice(MemberElement originalElement, final String name) {
        if (originalElement != null) {
            programModule.getTypeDefinition().getContext().getCompilationContext().error(originalElement, "Object '%s' defined twice", name);
        } else {
            programModule.getTypeDefinition().getContext().getCompilationContext().error("Synthetic object '%s' defined twice", name);
        }
    }

    public Function addFunction(ExecutableElement originalElement, String name, FunctionType type) {
        Function obj = new Function(
            originalElement, Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, type)
        );
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                definedObjects.put(name, obj);
            } else {
                if (existing instanceof FunctionDeclaration) {
                    if (! existing.getType().equals(obj.getType())) {
                        clash(originalElement, name);
                    } else {
                        definedObjects.replace(name, existing, obj);
                    }
                } else if (existing instanceof Function) {
                    twice(originalElement, name);
                } else {
                    clash(originalElement, name);
                }
            }
        }
        return obj;
    }

    public FunctionDeclaration declareFunction(ExecutableElement originalElement, String name, FunctionType type) {
        Assert.checkNotNullParam("name", name);
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                FunctionDeclaration decl = new FunctionDeclaration(
                    originalElement, name,
                    programModule.literalFactory.literalOfSymbol(name, type)
                );
                definedObjects.put(name, decl);
                return decl;
            } else {
                if (existing instanceof FunctionDeclaration) {
                    if (! type.equals(((FunctionDeclaration) existing).getType())) {
                        clash(originalElement, name);
                    }
                    return (FunctionDeclaration) existing;
                } else if (existing instanceof Function) {
                    if (! type.equals(((Function) existing).getType())) {
                        clash(originalElement, name);
                    }
                    return ((Function) existing).getDeclaration();
                } else {
                    clash(originalElement, name);
                    return new FunctionDeclaration(
                        originalElement, name,
                        programModule.literalFactory.literalOfSymbol(name, type)
                    );
                }
            }
        }
    }

    public DataDeclaration declareData(MemberElement originalElement, String name, ValueType type) {
        Assert.checkNotNullParam("name", name);
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                DataDeclaration decl = new DataDeclaration(originalElement, name,
                    programModule.literalFactory.literalOfSymbol(name, type)
                );
                definedObjects.put(name, decl);
                return decl;
            } else {
                if (existing instanceof DataDeclaration) {
                    if (! type.equals(existing.getType())) {
                        clash(originalElement, name);
                    }
                    return (DataDeclaration) existing;
                } else if (existing instanceof Data) {
                    if (! type.equals(existing.getType())) {
                        clash(originalElement, name);
                    }
                    return ((Data) existing).getDeclaration();
                } else {
                    clash(originalElement, name);
                    return new DataDeclaration(originalElement, name,
                        programModule.literalFactory.literalOfSymbol(name, type)
                    );
                }
            }
        }
    }

    private void clash(final MemberElement originalElement, final String name) {
        if (originalElement != null) {
            originalElement.getEnclosingType().getContext().getCompilationContext().error(originalElement, "Object '%s' redeclared with different type", name);
        } else {
            programModule.getTypeDefinition().getContext().getCompilationContext().error("Synthetic object '%s' redeclared with different type", name);
        }
    }

    public Iterable<ProgramObject> contents() {
        return List.of(definedObjects.values().toArray(ProgramObject[]::new));
    }
}
