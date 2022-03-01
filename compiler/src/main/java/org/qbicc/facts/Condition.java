package org.qbicc.facts;

import java.util.function.ObjLongConsumer;

/**
 * A condition that, when met, triggers the execution of an action.
 *
 * @param <F> the fact type
 */
public abstract class Condition<F extends Fact<?>> {
    Condition() {}

    /**
     * A condition which is true when the given fact is present.
     *
     * @param fact the fact
     * @param <F> the fact type
     * @return the condition
     */
    public static <F extends Fact<?>> Condition<F> when(F fact) {
        return new Of<>(fact);
    }


    /**
     * A condition which is true when all of the given facts are present.
     *
     * @param fact1 the first fact
     * @param fact2 the second fact
     * @param <F> the fact type
     * @return the condition
     */
    public static <F extends Fact<?>> Condition<F> whenAll(F fact1, F fact2) {
        return new All2<>(fact1, fact2);
    }

    /**
     * A condition which is true when all of the given facts are present.
     *
     * @param fact1 the first fact
     * @param fact2 the second fact
     * @param fact3 the second fact
     * @param <F> the fact type
     * @return the condition
     */
    public static <F extends Fact<?>> Condition<F> whenAll(F fact1, F fact2, F fact3) {
        return new All3<>(fact1, fact2, fact3);
    }

    /**
     * A condition which is true when all of the given facts are present.
     *
     * @param facts the facts
     * @param <F> the fact type
     * @return the condition
     */
    @SafeVarargs
    public static <F extends Fact<?>> Condition<F> whenAll(F... facts) {
        return new AllMany<>(facts);
    }


    /**
     * A condition which is true when any of the given facts are present.
     *
     * @param fact1 the first fact
     * @param fact2 the second fact
     * @param <F> the fact type
     * @return the condition
     */
    public static <F extends Fact<?>> Condition<F> whenAny(F fact1, F fact2) {
        return new Any2<>(fact1, fact2);
    }

    /**
     * A condition which is true when any of the given facts are present.
     *
     * @param fact1 the first fact
     * @param fact2 the second fact
     * @param fact3 the third fact
     * @param <F> the fact type
     * @return the condition
     */
    public static <F extends Fact<?>> Condition<F> whenAny(F fact1, F fact2, F fact3) {
        return new Any3<>(fact1, fact2, fact3);
    }

    /**
     * A condition which is true when any of the given facts are present.
     *
     * @param facts the facts
     * @param <F> the fact type
     * @return the condition
     */
    @SafeVarargs
    public static <F extends Fact<?>> Condition<F> whenAny(F... facts) {
        return new AnyMany<>(facts);
    }


    /**
     * Get a condition which is true when both this condition and the given condition are true.
     *
     * @param other the other condition
     * @return the new condition
     */
    public Condition<F> and(Condition<F> other) {
        return new And<F>(this, other);
    }


    /**
     * Get a condition which is true when either this condition or the given condition are true.
     *
     * @param other the other condition
     * @return the new condition
     */
    public Condition<F> or(Condition<F> other) {
        return new Or<F>(this, other);
    }


    abstract ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next);

    static class Of<F extends Fact<?>> extends Condition<F> {
        final F fact1;

        Of(F fact1) {
            this.fact1 = fact1;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> next.accept(holder, bits | 1L << holder.getFactIndex(fact1));
        }
    }

    static class Any2<F extends Fact<?>> extends Of<F> {
        final F fact2;

        Any2(F fact1, F fact2) {
            super(fact1);
            this.fact2 = fact2;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> {
                next.accept(holder, bits | 1L << holder.getFactIndex(fact1));
                next.accept(holder, bits | 1L << holder.getFactIndex(fact2));
            };
        }
    }

    static class Any3<F extends Fact<?>> extends Any2<F> {
        final F fact3;

        Any3(F fact1, F fact2, F fact3) {
            super(fact1, fact2);
            this.fact3 = fact3;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> {
                next.accept(holder, bits | 1L << holder.getFactIndex(fact1));
                next.accept(holder, bits | 1L << holder.getFactIndex(fact2));
                next.accept(holder, bits | 1L << holder.getFactIndex(fact3));
            };
        }
    }

    static class AnyMany<F extends Fact<?>> extends Condition<F> {
        final F[] facts;

        AnyMany(F[] facts) {
            this.facts = facts;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> {
                for (F fact : facts) {
                    next.accept(holder, bits | 1L << holder.getFactIndex(fact));
                }
            };
        }
    }

    static class All2<F extends Fact<?>> extends Of<F> {
        final F fact2;

        All2(F fact1, F fact2) {
            super(fact1);
            this.fact2 = fact2;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> next.accept(holder, bits | 1L << holder.getFactIndex(fact1) | 1L << holder.getFactIndex(fact2));
        }
    }

    static class All3<F extends Fact<?>> extends All2<F> {
        final F fact3;

        All3(F fact1, F fact2, F fact3) {
            super(fact1, fact2);
            this.fact3 = fact3;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> next.accept(holder, bits | 1L << holder.getFactIndex(fact1) | 1L << holder.getFactIndex(fact2) | 1L << holder.getFactIndex(fact3));
        }
    }

    static class AllMany<F extends Fact<?>> extends Condition<F> {
        final F[] facts;

        AllMany(F[] facts) {
            this.facts = facts;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return (holder, bits) -> {
                for (F fact : facts) {
                    bits |= 1L << holder.getFactIndex(fact);
                }
                next.accept(holder, bits);
            };
        }
    }

    static class Or<F extends Fact<?>> extends Condition<F> {
        private final Condition<F> cond1;
        private final Condition<F> cond2;

        Or(Condition<F> cond1, Condition<F> cond2) {
            this.cond1 = cond1;
            this.cond2 = cond2;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            var f1 = cond1.getRegisterFunction(next);
            var f2 = cond2.getRegisterFunction(next);
            return (holder, bits) -> {
                f1.accept(holder, bits);
                f2.accept(holder, bits);
            };
        }
    }

    static class And<F extends Fact<?>> extends Condition<F> {
        private final Condition<F> cond1;
        private final Condition<F> cond2;

        And(Condition<F> cond1, Condition<F> cond2) {
            this.cond1 = cond1;
            this.cond2 = cond2;
        }

        @Override
        ObjLongConsumer<Facts> getRegisterFunction(ObjLongConsumer<Facts> next) {
            return cond1.getRegisterFunction(cond2.getRegisterFunction(next));
        }
    }
}
