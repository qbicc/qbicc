package org.qbicc.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MemberElement;

/**
 *
 */
public final class ProgramModule {
    final DefinedTypeDefinition typeDefinition;
    final TypeSystem typeSystem;
    final LiteralFactory literalFactory;
    // All module objects by name, in no particular order
    // protected by this
    final Map<String, ProgramObject> moduleObjects = new HashMap<>();
    // All module functions in order (all sections)
    // protected by this
    final List<Function> functions = new ArrayList<>();
    // concurrent
    final Map<Section, ModuleSection> sections = new ConcurrentHashMap<>();
    // protected by ctors
    final List<GlobalXtor> ctors = new ArrayList<>();
    // protected by dtors
    final List<GlobalXtor> dtors = new ArrayList<>();

    public ProgramModule(final DefinedTypeDefinition typeDefinition, final TypeSystem typeSystem, final LiteralFactory literalFactory) {
        this.typeDefinition = Assert.checkNotNullParam("typeDefinition", typeDefinition);
        this.typeSystem = Assert.checkNotNullParam("typeSystem", typeSystem);
        this.literalFactory = Assert.checkNotNullParam("literalFactory", literalFactory);
    }

    public DefinedTypeDefinition getTypeDefinition() {
        return typeDefinition;
    }

    public Collection<ModuleSection> sections() {
        ModuleSection[] array = this.sections.values().toArray(ModuleSection[]::new);
        Arrays.sort(array, Comparator.comparing(ProgramObject::getName));
        return List.of(array);
    }

    public List<GlobalXtor> constructors() {
        synchronized (ctors) {
            return List.copyOf(ctors);
        }
    }

    public List<GlobalXtor> destructors() {
        synchronized (dtors) {
            return List.copyOf(dtors);
        }
    }

    public GlobalXtor addConstructor(Function fn, int priority) {
        Assert.checkNotNullParam("fn", fn);
        Assert.checkMinimumParameter("priority", 0, priority);
        GlobalXtor xtor = new GlobalXtor(GlobalXtor.Kind.CTOR, fn, priority);
        synchronized (ctors) {
            ctors.add(xtor);
        }
        return xtor;
    }

    public GlobalXtor addDestructor(Function fn, int priority) {
        Assert.checkNotNullParam("fn", fn);
        Assert.checkMinimumParameter("priority", 0, priority);
        GlobalXtor xtor = new GlobalXtor(GlobalXtor.Kind.DTOR, fn, priority);
        synchronized (dtors) {
            dtors.add(xtor);
        }
        return xtor;
    }

    public ModuleSection inSection(final Section section) {
        return sections.computeIfAbsent(Assert.checkNotNullParam("section", section), this::registerSection);
    }

    /**
     * Convenience method to produce a function declaration in this module for the given original program object, which
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
     * Convenience method to produce a function declaration in this module for the given original function.
     *
     * @param originalFunction the original function (must not be {@code null})
     * @return the function declaration (not {@code null})
     */
    public FunctionDeclaration declareFunction(Function originalFunction) {
        Assert.checkNotNullParam("originalFunction", originalFunction);
        String name = originalFunction.getName();
        Map<String, ProgramObject> definedObjects = this.moduleObjects;
        synchronized (this) {
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
     * Convenience method to produce a function declaration in this module for the given original function declaration.
     *
     * @param originalDecl the original function declaration (must not be {@code null})
     * @return the function declaration (not {@code null})
     */
    public FunctionDeclaration declareFunction(FunctionDeclaration originalDecl) {
        Assert.checkNotNullParam("originalDecl", originalDecl);
        String name = originalDecl.getName();
        Map<String, ProgramObject> definedObjects = this.moduleObjects;
        synchronized (this) {
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
        return declareFunction(originalElement, name, type, originalElement == null ? 0 : Function.getFunctionFlags(originalElement));
    }

    public FunctionDeclaration declareFunction(ExecutableElement originalElement, String name, FunctionType type, int flags) {
        return declareFunction(originalElement, name, type, flags, originalElement == null ? SafePointBehavior.ENTER : originalElement.safePointBehavior());
    }

    public FunctionDeclaration declareFunction(ExecutableElement originalElement, String name, FunctionType type, int flags, SafePointBehavior safePointBehavior) {
        Assert.checkNotNullParam("name", name);
        Map<String, ProgramObject> definedObjects = this.moduleObjects;
        synchronized (this) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                FunctionDeclaration decl = new FunctionDeclaration(originalElement, this, name, type, flags, safePointBehavior);
                definedObjects.put(name, decl);
                return decl;
            } else {
                if (existing instanceof FunctionDeclaration decl) {
                    if (! type.equals(decl.getValueType())) {
                        clash(originalElement, name);
                        return new FunctionDeclaration(originalElement, this, name, type, flags, safePointBehavior);
                    }
                    return decl;
                } else if (existing instanceof Function fn) {
                    if (! type.equals(fn.getValueType())) {
                        clash(originalElement, name);
                        return new FunctionDeclaration(originalElement, this, name, type, flags, safePointBehavior);
                    }
                    return fn.getDeclaration();
                } else {
                    clash(originalElement, name);
                    return new FunctionDeclaration(originalElement, this, name, type, flags, safePointBehavior);
                }
            }
        }
    }

    /**
     * Convenience method to produce a data declaration in this module for the given original program object, which
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
     * Convenience method to produce a data declaration in this module for the given original data definition.
     *
     * @param originalData the original data definition (must not be {@code null})
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration declareData(Data originalData) {
        Assert.checkNotNullParam("originalData", originalData);
        String name = originalData.getName();
        Map<String, ProgramObject> definedObjects = this.moduleObjects;
        synchronized (this) {
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
     * Convenience method to produce a data declaration in this module for the given original data declaration.
     *
     * @param originalDecl the original data declaration (must not be {@code null})
     * @return the data declaration (not {@code null})
     */
    public DataDeclaration declareData(DataDeclaration originalDecl) {
        Assert.checkNotNullParam("originalDecl", originalDecl);
        String name = originalDecl.getName();
        Map<String, ProgramObject> definedObjects = this.moduleObjects;
        synchronized (this) {
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
        Map<String, ProgramObject> definedObjects = this.moduleObjects;
        synchronized (this) {
            ProgramObject existing = definedObjects.get(name);
            if (existing == null) {
                DataDeclaration decl = new DataDeclaration(originalElement, this, name, type);
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
                    return new DataDeclaration(originalElement, this, name, type);
                }
            }
        }
    }

    void clash(final MemberElement originalElement, final String name) {
        if (originalElement != null) {
            originalElement.getEnclosingType().getContext().getCompilationContext().error(originalElement, "Object '%s' redeclared with different type", name);
        } else {
            getTypeDefinition().getContext().getCompilationContext().error("Synthetic object '%s' redeclared with different type", name);
        }
    }

    private ModuleSection registerSection(final Section section) {
        return new ModuleSection(section, typeSystem.getVoidType(), this);
    }

    public Iterable<Declaration> declarations() {
        return new Iterable<Declaration>() {
            @Override
            public Iterator<Declaration> iterator() {
                List<Declaration> decls;
                synchronized (ProgramModule.this) {
                    decls = new ArrayList<>(moduleObjects.size());
                    for (ProgramObject programObject : moduleObjects.values()) {
                        if (programObject instanceof Declaration decl) {
                            decls.add(decl);
                        }
                    }
                }
                decls.sort(Comparator.comparing(ProgramObject::getName));
                return decls.iterator();
            }
        };
    }

    public Function getFunction(final int fnIndex) {
        synchronized (this) {
            return functions.get(fnIndex);
        }
    }
}
