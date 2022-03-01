package org.qbicc.facts;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.PhaseAttachmentKey;

/**
 * A central point for registering and tracking facts about types and members.
 */
public final class Facts {
    private static final VarHandle factIdsHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "factIds", VarHandle.class, Facts.class, ImmutableObjectIntMap.class);
    private static final VarHandle longArrayHandle = ConstantBootstraps.arrayVarHandle(MethodHandles.lookup(), "_", VarHandle.class, long[].class);

    private final CompilationContext ctxt;

    private static final AttachmentKey<Facts> KEY = new AttachmentKey<>();

    private static final PhaseAttachmentKey<PerPhase> PER_PHASE_KEY = new PhaseAttachmentKey<>();

    @SuppressWarnings("FieldMayBeFinal") // VarHandle
    private volatile ImmutableObjectIntMap<Fact<?>> factIds = ObjectIntMaps.immutable.empty();

    static final class PerPhase {
        // The array value contains two elements:
        //   [0] - bits representing discovered facts
        //   [1] - bits representing executed actions
        // It seems like it would be possible to deduce the actions from the old and new facts, however this doesn't cover late-added actions
        final Map<Object, long[]> facts = new ConcurrentHashMap<>();
        final Map<Fact<?>, long[]> counts = new ConcurrentHashMap<>();
        volatile ImmutableList<Action<?>> actions = Lists.immutable.empty();

        PerPhase() {}
    }

    private Facts(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static Facts get(CompilationContext ctxt) {
        Facts facts = ctxt.getAttachment(KEY);
        if (facts == null) {
            facts = new Facts(ctxt);
            Facts appearing = ctxt.putAttachmentIfAbsent(KEY, facts);
            if (appearing != null) {
                facts = appearing;
            }
        }
        return facts;
    }

    int getFactIndex(Fact<?> fact) {
        int id;
        ImmutableObjectIntMap<Fact<?>> oldMap, newMap;
        for (;;) {
            oldMap = factIds;
            id = oldMap.getIfAbsent(fact, -1);
            if (id != -1) {
                return id;
            }
            id = oldMap.size();
            // if we need more, we'll have to do a slight design adjustment in order to preserve atomicity across multi-word RMW
            if (id >= 64) {
                throw new IllegalStateException("Too many facts");
            }
            newMap = oldMap.newWithKeyValue(fact, id);
            if (factIdsHandle.compareAndSet(this, oldMap, newMap)) {
                return id;
            }
        }
    }

    private PerPhase getPerPhase() {
        PerPhase perPhase = ctxt.getAttachment(PER_PHASE_KEY);
        if (perPhase == null) {
            perPhase = new PerPhase();
            PerPhase appearing = ctxt.putAttachmentIfAbsent(PER_PHASE_KEY, perPhase);
            if (appearing != null) {
                perPhase = appearing;
            }
        }
        return perPhase;
    }

    private PerPhase getPreviousPerPhase() {
        return ctxt.getPreviousPhaseAttachment(PER_PHASE_KEY);
    }

    private void registerAction(long bits, BiConsumer<?, Facts> consumer) {
        PerPhase perPhase = getPerPhase();
        synchronized (perPhase) {
            Action<?> action = new Action<>(bits, consumer);
            ImmutableList<Action<?>> newActions = perPhase.actions.newWith(action);
            perPhase.actions = newActions;
            Map<Object, long[]> factsMap = perPhase.facts;
            if (! factsMap.isEmpty()) {
                // execute all newly-registered actions
                int actionBit = newActions.size() - 1;
                for (Map.Entry<Object, long[]> entry : factsMap.entrySet()) {
                    long[] array = entry.getValue();
                    long factBits = (long) longArrayHandle.getVolatile(array, 0);
                    if (action.isNewlySatisfiedBy(0, factBits)) {
                        long oldVal;
                        long newVal;
                        for (;;) {
                            oldVal = (long) longArrayHandle.getVolatile(array, 1);
                            newVal = oldVal | 1L << actionBit;
                            if (oldVal == newVal) {
                                break;
                            }
                            if (longArrayHandle.compareAndSet(array, 1, oldVal, newVal)) {
                                action.accept(entry.getKey());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static long[] newArray(Object ignored) {
        // [0] = fact bits
        // [1] = action bits
        return new long[2];
    }

    private <E> long discover(E item, long factBits) {
        PerPhase perPhase = getPerPhase();
        Map<Object, long[]> facts = perPhase.facts;
        long[] array = facts.computeIfAbsent(item, Facts::newArray);
        long oldFacts, newFacts;
        do {
            oldFacts = (long) longArrayHandle.getVolatile(array, 0);
            if ((oldFacts & factBits) == factBits) {
                // already known
                return 0;
            }
            newFacts = oldFacts | factBits;
        } while (! longArrayHandle.compareAndSet(array, 0, oldFacts, newFacts));
        // todo: optimize by utilizing newFacts ^ oldFacts to find the actions that could be newly satisfied
        ImmutableList<Action<?>> actions = perPhase.actions;
        int size = actions.size();
        for (int i = 0; i < size; i ++) {
            Action<?> action = actions.get(i);
            if (action.isNewlySatisfiedBy(oldFacts, newFacts)) {
                long oldVal;
                long newVal;
                for (;;) {
                    oldVal = (long) longArrayHandle.getVolatile(array, 1);
                    newVal = oldVal | 1L << i;
                    if (oldVal == newVal) {
                        break;
                    }
                    if (longArrayHandle.compareAndSet(array, 1, oldVal, newVal)) {
                        ctxt.submitTask(item, action);
                        break;
                    }
                }
            }
        }
        return newFacts & ~oldFacts;
    }

    public <E> void discover(E item, Fact<? super E> fact) {
        // type check
        fact.getElementType().cast(item);
        if (discover(item, 1L << getFactIndex(fact)) != 0) {
            longArrayHandle.getAndAdd(getPerPhase().counts.computeIfAbsent(fact, Facts::newArray), 0, 1L);
        }
    }

    public <E> void discover(E item, Fact<? super E> fact1, Fact<? super E> fact2) {
        // type check
        fact1.getElementType().cast(item);
        fact2.getElementType().cast(item);
        long bit1 = 1L << getFactIndex(fact1);
        long bit2 = 1L << getFactIndex(fact2);
        long newly = discover(item, bit1 | bit2);
        PerPhase perPhase = getPerPhase();
        if ((newly & bit1) != 0) {
            longArrayHandle.getAndAdd(perPhase.counts.computeIfAbsent(fact1, Facts::newArray), 0, 1L);
        }
        if ((newly & bit2) != 0) {
            longArrayHandle.getAndAdd(perPhase.counts.computeIfAbsent(fact2, Facts::newArray), 0, 1L);
        }
    }

    public <E> void discover(E item, Fact<? super E> fact1, Fact<? super E> fact2, Fact<? super E> fact3) {
        // type check
        fact1.getElementType().cast(item);
        fact2.getElementType().cast(item);
        fact3.getElementType().cast(item);
        long bit1 = 1L << getFactIndex(fact1);
        long bit2 = 1L << getFactIndex(fact2);
        long bit3 = 1L << getFactIndex(fact3);
        long newly = discover(item, bit1 | bit2 | bit3);
        PerPhase perPhase = getPerPhase();
        if ((newly & bit1) != 0) {
            longArrayHandle.getAndAdd(perPhase.counts.computeIfAbsent(fact1, Facts::newArray), 0, 1L);
        }
        if ((newly & bit2) != 0) {
            longArrayHandle.getAndAdd(perPhase.counts.computeIfAbsent(fact2, Facts::newArray), 0, 1L);
        }
        if ((newly & bit3) != 0) {
            longArrayHandle.getAndAdd(perPhase.counts.computeIfAbsent(fact3, Facts::newArray), 0, 1L);
        }
    }

    public long getDiscoveredCount(Fact<?> fact) {
        long[] array = getPerPhase().counts.get(fact);
        return array == null ? 0 : (long) longArrayHandle.getVolatile(array, 0);
    }

    public <E> boolean isDiscovered(final E item, final Fact<? super E> fact) {
        long[] array = getPerPhase().facts.get(item);
        long factBits = 1L << getFactIndex(fact);
        return array != null && fact.getElementType().isInstance(item) && (array[0] & factBits) == factBits;
    }

    private <E> boolean hadAllFactBits(E item, long factBits) {
        PerPhase previous = getPreviousPerPhase();
        if (previous == null) {
            return false;
        }
        long[] array = previous.facts.get(item);
        return array != null && (array[0] & factBits) == factBits;
    }

    private <E> boolean hadAnyFactBits(E item, long factBits) {
        PerPhase previous = getPreviousPerPhase();
        if (previous == null) {
            return false;
        }
        long[] array = previous.facts.get(item);
        return array != null && (array[0] & factBits) != 0;
    }

    public <E> boolean hadFact(E item, Fact<? super E> fact) {
        return fact.getElementType().isInstance(item) && hadAllFactBits(item, 1L << getFactIndex(fact));
    }

    public <E> boolean hadAllFacts(E item, Fact<? super E> fact1, Fact<? super E> fact2) {
        return fact1.getElementType().isInstance(item) && fact2.getElementType().isInstance(item) && hadAllFactBits(item, 1L << getFactIndex(fact1) | 1L << getFactIndex(fact2));
    }

    public <E> boolean hadAllFacts(E item, Fact<? super E> fact1, Fact<? super E> fact2, Fact<? super E> fact3) {
        return fact1.getElementType().isInstance(item) && fact2.getElementType().isInstance(item) && fact3.getElementType().isInstance(item) && hadAllFactBits(item, 1L << getFactIndex(fact1) | 1L << getFactIndex(fact2) | 1L << getFactIndex(fact3));
    }

    public <E> boolean hadAnyFacts(E item, Fact<? super E> fact1, Fact<? super E> fact2) {
        return fact1.getElementType().isInstance(item) && fact2.getElementType().isInstance(item) && hadAnyFactBits(item, 1L << getFactIndex(fact1) | 1L << getFactIndex(fact2));
    }

    public <E> boolean hadAnyFacts(E item, Fact<? super E> fact1, Fact<? super E> fact2, Fact<? super E> fact3) {
        return fact1.getElementType().isInstance(item) && fact2.getElementType().isInstance(item) && fact3.getElementType().isInstance(item) && hadAnyFactBits(item, 1L << getFactIndex(fact1) | 1L << getFactIndex(fact2) | 1L << getFactIndex(fact3));
    }

    public <E> void registerAction(Condition<? extends Fact<? super E>> condition, BiConsumer<E, Facts> action) {
        condition.getRegisterFunction((facts, bits) -> facts.registerAction(bits, action)).accept(this, 0);
    }

    public <E> void registerAction(Condition<? extends Fact<? super E>> condition, Consumer<E> action) {
        registerAction(condition, (e, f) -> action.accept(e));
    }

    public CompilationContext getCompilationContext() {
        return ctxt;
    }

    public boolean hasPreviousFacts() {
        return ctxt.getPreviousPhaseAttachment(PER_PHASE_KEY) != null;
    }

    final class Action<K> implements Consumer<Object> {
        private final long requiredBits;
        private final BiConsumer<K, Facts> consumer;

        Action(long requiredBits, BiConsumer<K, Facts> consumer) {
            this.requiredBits = requiredBits;
            this.consumer = consumer;
        }

        boolean isNewlySatisfiedBy(long oldBits, long newBits) {
            long requiredBits = this.requiredBits;
            return (oldBits & requiredBits) != requiredBits && (newBits & requiredBits) == requiredBits;
        }

        @SuppressWarnings("unchecked")
        public void accept(final Object item) {
            consumer.accept((K) item, Facts.this);
        }
    }
}
