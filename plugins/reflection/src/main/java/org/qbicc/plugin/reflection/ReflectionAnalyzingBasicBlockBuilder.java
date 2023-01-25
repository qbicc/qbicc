package org.qbicc.plugin.reflection;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;
import java.util.Map;

/**
 * This BBB provides automatic registration/folding of reflective operations
 * with constant arguments. Like GraalVM's "Automatic Detection"
 * of reflection (https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/),
 * the goal is to support simple patterns for reflection without explicit user annotation.
 */
public class ReflectionAnalyzingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final AttachmentKey<KnownMethods> KEY = new AttachmentKey<>();


    private final CompilationContext ctxt;

    public ReflectionAnalyzingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        Value replacement = process(targetPtr, receiver, arguments);
        return replacement != null ? replacement : super.call(targetPtr, receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        Value replacement = process(targetPtr, receiver, arguments);
        return replacement != null ? replacement : super.callNoSideEffects(targetPtr, receiver, arguments);
    }


    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        Value replacement = process(targetPtr, receiver, arguments);
        return replacement != null ?  replacement : super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private Value process(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr instanceof ExecutableLiteral el) {
            KnownMethods km = KnownMethods.get(ctxt);

            // If the target is a method of java.lang.Class, then see if it is one of the reflection entry points
            // we want to handle specially.
            if (el.getExecutable() instanceof MethodElement me && me.getEnclosingType().load().equals(km.jlc)) {
                // replace Class.forName(<literal String>) with a class literal if the type can be resolved at build time
                if (me.equals(km.forName) && arguments.get(0) instanceof StringLiteral sl) {
                    String internalName = sl.getValue().replace('.', '/');
                    DefinedTypeDefinition dtd = getCurrentClassContext().findDefinedType(internalName);
                    if (dtd != null) {
                        Value cls = getFirstBuilder().classOf(dtd.load().getObjectType());
                        getFirstBuilder().initializeClass(cls); // Class.forName causes class initialization; preserve those semantics for interpreter
                        return cls;
                    }
                }

                if (receiver instanceof ClassOf co && co.getInput() instanceof TypeLiteral tl && tl.getValue() instanceof ObjectType ot) {
                    // We know the receiving class precisely at compile time; optimize/auto-register
                    LoadedTypeDefinition receivingClass = ot.getDefinition().load();

                    if (me.equals(km.getField)) {
                        if (arguments.get(0) instanceof StringLiteral sl) {
                            // TODO: findField only looks in receivingClass while Class.getField(String) should do a full resolution
                            //       We don't have the descriptor available that would allow us to use resolveField
                            FieldElement fe = receivingClass.findField(sl.getValue());
                            if (fe != null) {
                                VmObject fObj = Reflection.get(ctxt).getField(fe);
                                return ctxt.getLiteralFactory().literalOf(fObj);
                            }
                        }
                        // Not able to resolve; ensure receivingClass is ready for runtime reflection on its fields
                        registerForReflection(receivingClass, true, false, false);
                        return null;
                    }

                    if (me.equals(km.getDeclaredField)) {
                        if (arguments.get(0) instanceof StringLiteral sl) {
                            FieldElement fe = receivingClass.findField(sl.getValue());
                            if (fe != null) {
                                VmObject fObj = Reflection.get(ctxt).getField(fe);
                                return ctxt.getLiteralFactory().literalOf(fObj);
                            }
                            // TODO: this could be turned into a throw of NoSuchFieldException
                        }
                        // Not able to resolve; ensure receivingClass is ready for runtime reflection on its fields
                        registerForReflection(receivingClass, true, false, false);
                        return null;
                    }

                    if (me.equals(km.getConstructor) || me.equals(km.getDeclaredConstructor)) {
                        // TODO: To eliminate the reflection, we need arguments(0) to be an ArrayLiteral all of whose elements are ClassLiterals
                        registerForReflection(receivingClass, false, true, false);
                        return null;
                    }

                    if (me.equals(km.getMethod) || me.equals(km.getDeclaredMethod)) {
                        // TODO: To eliminate the reflection, we need arguments(0) to be a StringLiteral and
                        //       arguments(1) to be an ArrayLiteral all of whose elements are ClassLiterals
                        registerForReflection(receivingClass, false, false, true);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private void registerForReflection(LoadedTypeDefinition cls, boolean fields, boolean constructors, boolean methods) {
        // System.out.println("Registered "+cls+ " as reflective");
        ReachabilityRoots roots = ReachabilityRoots.get(ctxt);
        Reflection reflect = Reflection.get(ctxt);
        roots.registerReflectiveClass(cls);
        if (fields) {
            // System.out.println("Registered the fields of "+cls+ " as reflective");
            int fc = cls.getFieldCount();
            for (int i = 0; i < fc; i++) {
                roots.registerReflectiveField(cls.getField(i));
                reflect.makeAvailableForRuntimeReflection(cls.getField(i));
            }
        }
        if (constructors) {
            // System.out.println("Registered the constructors of "+cls+ " as reflective");
            int cc = cls.getConstructorCount();
            for (int i = 0; i < cc; i++) {
                roots.registerReflectiveConstructor(cls.getConstructor(i));
                reflect.makeAvailableForRuntimeReflection(cls.getConstructor(i));
            }
        }
        if (methods) {
            // System.out.println("Registered the methods of "+cls+ " as reflective");
            int mc = cls.getMethodCount();
            for (int i = 0; i < mc; i++) {
                roots.registerReflectiveMethod(cls.getMethod(i));
                reflect.makeAvailableForRuntimeReflection(cls.getMethod(i));
            }
            for (MethodElement m: cls.getInstanceMethods()) {
                if (!m.isAbstract() && !m.isNative()) {
                    roots.registerReflectiveMethod(m);
                    reflect.makeAvailableForRuntimeReflection(m);
                }
            }
        }
    }

    private static final class KnownMethods {
        final LoadedTypeDefinition jlc;
        final MethodElement forName;
        final MethodElement getConstructor;
        final MethodElement getDeclaredConstructor;
        final MethodElement getField;
        final MethodElement getDeclaredField;
        final MethodElement getMethod;
        final MethodElement getDeclaredMethod;

        private KnownMethods(CompilationContext ctxt) {
            jlc = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
            forName = jlc.requireSingleMethod("forName", 1);
            getConstructor = jlc.requireSingleMethod("getConstructor", 1);
            getDeclaredConstructor = jlc.requireSingleMethod("getDeclaredConstructor", 1);
            getField = jlc.requireSingleMethod("getField", 1);
            getDeclaredField = jlc.requireSingleMethod("getDeclaredField", 1);
            getMethod = jlc.requireSingleMethod("getMethod", 2);
            getDeclaredMethod = jlc.requireSingleMethod("getDeclaredMethod", 2);
        }

        static KnownMethods get(CompilationContext ctxt) {
            KnownMethods km = ctxt.getAttachment(KEY);
            if (km == null) {
                km = new KnownMethods(ctxt);
                KnownMethods appearing = ctxt.putAttachmentIfAbsent(KEY, km);
                if (appearing != null) {
                    km = appearing;
                }
            }
            return km;
        }
    }
}
