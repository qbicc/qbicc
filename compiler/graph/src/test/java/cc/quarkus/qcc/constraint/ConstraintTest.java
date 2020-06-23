package cc.quarkus.qcc.constraint;

import org.junit.Test;

import static cc.quarkus.qcc.constraint.Constraint.*;
import static org.fest.assertions.api.Assertions.*;

public class ConstraintTest {

    @Test
    public void testArrayIndexExample() {
        ValueImpl someRandomNumberThing = new ValueImpl("num");
        ValueImpl arrayLength = new ValueImpl("array.length");

        ValueImpl i = new ValueImpl("i");
        i.setConstraint(lessThan(arrayLength).intersect(greaterThanOrEqualTo(someRandomNumberThing)));

        Satisfaction result = null;

        result = lessThan(arrayLength).isSatisfiedBy(i);
        assertThat(result).isSameAs(Satisfaction.YES);

        //ValueImpl j = new ValueImpl();
        //result = lessThan(arrayLength).isSatisfiedBy(j.getConstraint());
        //assertThat(result).isSameAs(Satisfaction.NO);
    }

    @Test
    public void testTransitivity() {
        ValueImpl zero = new ValueImpl("0");
        ValueImpl arrayLength = new ValueImpl("array.length");

        ValueImpl i = new ValueImpl("i");
        i.setConstraint(lessThan(arrayLength).intersect(greaterThanOrEqualTo(zero)));

        ValueImpl j = new ValueImpl("j");
        j.setConstraint(lessThanOrEqualTo(i));

        Satisfaction result = lessThan(arrayLength).intersect(greaterThanOrEqualTo(zero)).isSatisfiedBy(j);
        assertThat(result).isSameAs(Satisfaction.YES);
    }

    @Test
    public void testTransitivityMultipleIntersections() {
        ValueImpl zero = new ValueImpl("0");
        ValueImpl arrayLength = new ValueImpl("array.length");

        ValueImpl i = new ValueImpl("i");
        i.setConstraint(lessThan(arrayLength).intersect(greaterThanOrEqualTo(zero)));

        ValueImpl j = new ValueImpl("j");
        j.setConstraint(lessThanOrEqualTo(i));

        Satisfaction result = lessThan(arrayLength).intersect(greaterThan(zero)).isSatisfiedBy(j);
        assertThat(result).isSameAs(Satisfaction.NO);
    }

    @Test
    public void testConstantness() {
        ValueImpl num1 = new ValueImpl("num1");
        num1.setConstraint(greaterThanOrEqualTo(num1).intersect(lessThanOrEqualTo(num1)));

        ValueImpl num2 = new ValueImpl("num2");
        num2.setConstraint(greaterThanOrEqualTo(num2).intersect(lessThanOrEqualTo(num2)));

        ValueImpl range = new ValueImpl("range");
        range.setConstraint(greaterThanOrEqualTo(num1).intersect(lessThanOrEqualTo(num2)));

        SymbolicValueImpl sym = new SymbolicValueImpl("$sym");

        assertThat(equalTo(sym).isSatisfiedBy(num1)).isSameAs(Satisfaction.YES);
        assertThat(equalTo(sym).isSatisfiedBy(num2)).isSameAs(Satisfaction.YES);
        assertThat(equalTo(sym).isSatisfiedBy(range)).isSameAs(Satisfaction.NO);
    }

    @Test
    public void testRangeExtraction() {
        ValueImpl num1 = new ValueImpl("num1");
        ValueImpl num2 = new ValueImpl("num2");

        ValueImpl num1prime = new ValueImpl("num1prime");
        ValueImpl num2prime = new ValueImpl("num2prime");

        num1prime.setConstraint( lessThan(num1));
        num2prime.setConstraint( greaterThan(num2));

        ValueImpl target = new ValueImpl("target");
        target.setConstraint( greaterThan(num1).intersect(lessThan(num2)));

        SymbolicValueImpl low = new SymbolicValueImpl("$low");
        SymbolicValueImpl high = new SymbolicValueImpl("$high");

        SatisfactionContextImpl ctx = new SatisfactionContextImpl(target);

        Satisfaction result = greaterThan(low).intersect(lessThan(high)).isSatisfiedBy(ctx);

        assertThat(result).isSameAs(Satisfaction.YES);
        assertThat(ctx.getBinding(low)).isSameAs(num1);
        assertThat(ctx.getBinding(high)).isSameAs(num2);
    }
}
