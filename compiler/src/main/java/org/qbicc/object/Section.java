package org.qbicc.object;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.graph.Value;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MemberElement;
import io.smallrye.common.constraint.Assert;

/**
 * A section in a program.
 */
public final class Section extends ProgramObject {
    final ProgramModule programModule;
    final Map<String, ProgramObject> definedObjects = Collections.synchronizedMap(new LinkedHashMap<>());

    Section(final String name, final ValueType valueType, final ProgramModule programModule) {
        super(name, valueType);
        this.programModule = programModule;
    }

    public Data addData(MemberElement originalElement, String name, Value value) {
        Data obj = new Data(originalElement,
            Assert.checkNotNullParam("name", name),
            Assert.checkNotNullParam("value", value).getType(),
            value
        );
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                definedObjects.put(name, obj);
            } else {
                if (existing instanceof DataDeclaration decl) {
                    if (! decl.getValueType().equals(obj.getValueType())) {
                        clash(originalElement, name);
                    } else {
                        obj.initDeclaration(decl);
                        definedObjects.replace(name, decl, obj);
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
        Function obj = new Function(originalElement,
            Assert.checkNotNullParam("name", name),
            Assert.checkNotNullParam("type", type),
            Function.getFunctionFlags(originalElement)
        );
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                definedObjects.put(name, obj);
            } else {
                if (existing instanceof FunctionDeclaration decl) {
                    if (! decl.getSymbolType().equals(obj.getSymbolType())) {
                        clash(originalElement, name);
                    } else {
                        obj.initDeclaration(decl);
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

    /**
     * Convenience method to produce a function declaration in this section for the given original program object, which
     * must be a function definition or a function declaration.
     *
     * @param original the original item (must not be {@code null})
     * @return the function declaration (not {@code null})
     */
    public FunctionDeclaration declareFunction(ProgramObject original) {
        if (original instanceof Function fn) {
            return declareFunction(fn);
        } else if (original instanceof FunctionDeclaration decl) {
            return declareFunction(decl);
        } else {
            throw new IllegalArgumentException("Invalid input type");
        }
    }

    /**
     * Convenience method to produce a function declaration in this section for the given original function.
     *
     * @param originalFunction the original function (must not be {@code null})
     * @return the function declaration (not {@code null})
     */
    public FunctionDeclaration declareFunction(Function originalFunction) {
        Assert.checkNotNullParam("originalFunction", originalFunction);
        String name = originalFunction.getName();
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            FunctionDeclaration origDecl = originalFunction.getDeclaration();
            if (existing == null) {
                definedObjects.put(name, origDecl);
                return origDecl;
            } else {
                if (existing == origDecl) {
                    // fast/common path
                    return origDecl;
                } else if (existing instanceof FunctionDeclaration decl) {
                    if (! originalFunction.getSymbolType().equals(decl.getSymbolType())) {
                        clash(originalFunction.getOriginalElement(), name);
                        return origDecl;
                    }
                    return decl;
                } else if (existing instanceof Function fn) {
                    if (! originalFunction.getSymbolType().equals(fn.getSymbolType())) {
                        clash(originalFunction.getOriginalElement(), name);
                        return origDecl;
                    }
                    return fn.getDeclaration();
                } else {
                    clash(originalFunction.getOriginalElement(), name);
                    return origDecl;
                }
            }
        }
    }

    /**
     * Convenience method to produce a function declaration in this section for the given original function declaration.
     *
     * @param originalDecl the original function declaration (must not be {@code null})
     * @return the function declaration (not {@code null})
     */
    public FunctionDeclaration declareFunction(FunctionDeclaration originalDecl) {
        Assert.checkNotNullParam("originalDecl", originalDecl);
        String name = originalDecl.getName();
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                definedObjects.put(name, originalDecl);
                return originalDecl;
            } else {
                if (existing == originalDecl) {
                    // fast/common path
                    return originalDecl;
                } else if (existing instanceof FunctionDeclaration decl) {
                    if (! originalDecl.getSymbolType().equals(decl.getSymbolType())) {
                        clash(originalDecl.getOriginalElement(), name);
                        return originalDecl;
                    }
                    return decl;
                } else if (existing instanceof Function fn) {
                    if (! originalDecl.getSymbolType().equals(fn.getSymbolType())) {
                        clash(originalDecl.getOriginalElement(), name);
                        return originalDecl.getDeclaration();
                    }
                    return fn.getDeclaration();
                } else {
                    clash(originalDecl.getOriginalElement(), name);
                    return originalDecl;
                }
            }
        }
    }

    public FunctionDeclaration declareFunction(ExecutableElement originalElement, String name, FunctionType type) {
        Assert.checkNotNullParam("name", name);
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                FunctionDeclaration decl = new FunctionDeclaration(originalElement, name, type);
                definedObjects.put(name, decl);
                return decl;
            } else {
                if (existing instanceof FunctionDeclaration decl) {
                    if (! type.equals(decl.getValueType())) {
                        clash(originalElement, name);
                        return new FunctionDeclaration(originalElement, name, type);
                    }
                    return decl;
                } else if (existing instanceof Function fn) {
                    if (! type.equals(fn.getValueType())) {
                        clash(originalElement, name);
                        return new FunctionDeclaration(originalElement, name, type);
                    }
                    return fn.getDeclaration();
                } else {
                    clash(originalElement, name);
                    return new FunctionDeclaration(originalElement, name, type);
                }
            }
        }
    }

    /**
     * Convenience method to produce a data declaration in this section for the given original program object, which
     * must be a data definition or a data declaration.
     *
     * @param original the original item (must not be {@code null})
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration declareData(ProgramObject original) {
        if (original instanceof Data data) {
            return declareData(data);
        } else if (original instanceof DataDeclaration decl) {
            return declareData(decl);
        } else {
            throw new IllegalArgumentException("Invalid input type");
        }
    }

    /**
     * Convenience method to produce a data declaration in this section for the given original data definition.
     *
     * @param originalData the original data definition (must not be {@code null})
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration declareData(Data originalData) {
        Assert.checkNotNullParam("originalData", originalData);
        String name = originalData.getName();
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            DataDeclaration origDecl = originalData.getDeclaration();
            if (existing == null) {
                definedObjects.put(name, origDecl);
                return origDecl;
            } else {
                if (existing == origDecl) {
                    // fast/common path
                    return origDecl;
                } else if (existing instanceof DataDeclaration decl) {
                    if (! originalData.getSymbolType().equals(decl.getSymbolType())) {
                        clash(originalData.getOriginalElement(), name);
                        return origDecl;
                    }
                    return decl;
                } else if (existing instanceof Data data) {
                    if (! originalData.getSymbolType().equals(data.getSymbolType())) {
                        clash(originalData.getOriginalElement(), name);
                        return origDecl;
                    }
                    return data.getDeclaration();
                } else {
                    clash(originalData.getOriginalElement(), name);
                    return origDecl;
                }
            }
        }
    }

    /**
     * Convenience method to produce a data declaration in this section for the given original data declaration.
     *
     * @param originalDecl the original data declaration (must not be {@code null})
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration declareData(DataDeclaration originalDecl) {
        Assert.checkNotNullParam("originalDecl", originalDecl);
        String name = originalDecl.getName();
        Map<String, ProgramObject> definedObjects = this.definedObjects;
        synchronized (definedObjects) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                // reuse original decl
                definedObjects.put(name, originalDecl);
                return originalDecl;
            } else {
                if (existing == originalDecl) {
                    // fast/common path
                    return originalDecl;
                } else if (existing instanceof DataDeclaration decl) {
                    if (! originalDecl.getSymbolType().equals(decl.getSymbolType())) {
                        clash(originalDecl.getOriginalElement(), name);
                        return originalDecl;
                    }
                    return decl;
                } else if (existing instanceof Data data) {
                    if (! originalDecl.getSymbolType().equals(data.getSymbolType())) {
                        clash(originalDecl.getOriginalElement(), name);
                        return originalDecl;
                    }
                    return data.getDeclaration();
                } else {
                    clash(originalDecl.getOriginalElement(), name);
                    return originalDecl;
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
                DataDeclaration decl = new DataDeclaration(originalElement, name, type);
                definedObjects.put(name, decl);
                return decl;
            } else {
                if (existing instanceof DataDeclaration decl) {
                    if (! type.equals(decl.getValueType())) {
                        clash(originalElement, name);
                    }
                    return decl;
                } else if (existing instanceof Data data) {
                    if (! type.equals(data.getValueType())) {
                        clash(originalElement, name);
                    }
                    return data.getDeclaration();
                } else {
                    clash(originalElement, name);
                    return new DataDeclaration(originalElement, name, type);
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
        ProgramObject[] array = definedObjects.values().toArray(ProgramObject[]::new);
        Arrays.sort(array, Comparator.comparing(ProgramObject::getName));
        return List.of(array);
    }

    @Override
    public Section getDeclaration() {
        return this;
    }
}
