package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import org.qbicc.graph.literal.Literal;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.type.definition.classfile.ClassFile;

/**
 * A static field element.
 */
public final class StaticFieldElement extends FieldElement {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, StaticFieldElement.class, StaticFieldPointer.class);
    private final Literal initialValue;
    private final InitializerElement runTimeInitializer;
    private final String loweredName;
    @SuppressWarnings("unused") // pointerHandle
    private volatile StaticFieldPointer pointer;

    StaticFieldElement(BuilderImpl builder) {
        super(builder);
        initialValue = builder.initialValue;
        runTimeInitializer = builder.runTimeInitializer;
        loweredName = builder.loweredName;
    }

    @Override
    public boolean isReallyFinal() {
        return runTimeInitializer == null && super.isReallyFinal();
    }

    public Literal getInitialValue() {
        return initialValue;
    }

    @Override
    public void clearModifierFlags(int flags) {
        if ((flags & ClassFile.ACC_STATIC) != 0) {
            throw new IllegalArgumentException("Cannot make a static element into an instance element");
        }
        super.clearModifierFlags(flags);
    }

    public InitializerElement getRunTimeInitializer() {
        return runTimeInitializer;
    }

    /**
     * Get the lowered name of this field.
     * The lowered name is the name of the global variable symbol to use for this element.
     *
     * @return the lowered name (not {@code null})
     */
    public String getLoweredName() {
        return loweredName;
    }

    /**
     * Get the pointer to this (static) field.  Convenience method which delegates to {@link StaticFieldPointer#of}.
     *
     * @return the pointer
     * @throws IllegalArgumentException if this field is not static
     */
    public StaticFieldPointer getPointer() {
        return StaticFieldPointer.of(this);
    }

    /**
     * Establish the pointer for this field; intended only for use by {@link StaticFieldPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see StaticFieldPointer#of
     */
    public StaticFieldPointer getOrCreatePointer(Function<StaticFieldElement, StaticFieldPointer> factory) {
        StaticFieldPointer pointer = this.pointer;
        if (pointer == null) {
            if (! isStatic()) {
                throw new IllegalArgumentException("Static pointer for instance field");
            }
            pointer = factory.apply(this);
            StaticFieldPointer appearing = (StaticFieldPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
    }
}
