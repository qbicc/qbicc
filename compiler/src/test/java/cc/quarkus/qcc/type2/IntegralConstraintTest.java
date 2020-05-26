package cc.quarkus.qcc.type2;

import org.junit.Test;

import static cc.quarkus.qcc.type2.IntegralConstraint.*;
import static org.fest.assertions.api.Assertions.*;

public class IntegralConstraintTest {

    @Test
    public void testJoinExpandLower() {
        IntegralConstraint c1 = greaterThan(10);
        IntegralConstraint c2 = greaterThanOrEqual(5);
        IntegralConstraint c3 = c1.join(c2);

        assertThat(c3.getLower().getBound()).isEqualTo(5);
        assertThat(c3.getLower().getOp()).isSameAs(Op.INCLUSIVE);

        assertThat(c3.getUpper()).isNull();
    }

    @Test
    public void testJoinExpandUpper() {
        IntegralConstraint c1 = lessThan(100);
        IntegralConstraint c2 = lessThan(150);

        IntegralConstraint c3 = c1.join(c2);

        assertThat(c3.getLower()).isNull();

        assertThat(c3.getUpper().getBound()).isEqualTo(150);
        assertThat(c3.getUpper().getOp()).isSameAs(Op.EXCLUSIVE);
    }

    @Test
    public void testSimpleMeet() {
        IntegralConstraint c1 = greaterThan(10).meet(lessThan(100));

        assertThat(c1.getLower().getBound()).isEqualTo(10);
        assertThat(c1.getLower().getOp()).isSameAs(Op.EXCLUSIVE);
        assertThat(c1.getUpper().getBound()).isEqualTo(100);
        assertThat(c1.getUpper().getOp()).isSameAs(Op.EXCLUSIVE);
    }

    @Test
    public void testVennMeet() {
        IntegralConstraint c1 = greaterThanOrEqual(100).meet(lessThanOrEqual(1000));
        IntegralConstraint c2 = greaterThanOrEqual(5).meet(lessThan(999));

        IntegralConstraint c3 = c1.meet(c2);

        assertThat(c3.getLower().getBound()).isEqualTo(100);
        assertThat(c3.getLower().getOp()).isSameAs(Op.INCLUSIVE);

        assertThat(c3.getUpper().getBound()).isEqualTo(999);
        assertThat(c3.getUpper().getOp()).isSameAs(Op.EXCLUSIVE);
    }

    @Test
    public void testJoinTop() {
        IntegralConstraint c1 = greaterThanOrEqual(100);

        IntegralConstraint c2 = c1.join(TOP);
        assertThat(c2).isSameAs(TOP);

        c2 = TOP.join(c1);
        assertThat(c2).isSameAs(TOP);
    }

    @Test
    public void testMeetTop() {
        IntegralConstraint c1 = greaterThanOrEqual(100);

        IntegralConstraint c2 = c1.meet(TOP);
        assertThat(c2).isSameAs(c1);

        c2 = TOP.meet(c1);
        assertThat(c2).isSameAs(c1);
    }

    @Test
    public void testJoinBottom() {
        IntegralConstraint c1 = greaterThanOrEqual(100);
        IntegralConstraint c2 = c1.join(BOTTOM);

        assertThat(c2).isSameAs(c1);

        c2 = BOTTOM.join(c1);
        assertThat(c2).isSameAs(c1);
    }

    @Test
    public void testMeetBottom() {
        IntegralConstraint c1 = greaterThanOrEqual(100);
        IntegralConstraint c2 = c1.meet(BOTTOM);

        assertThat(c2).isSameAs(BOTTOM);

        c2 = BOTTOM.meet(c1);
        assertThat(c2).isSameAs(BOTTOM);
    }

    @Test
    public void testIncompatibleMeet() {
        IntegralConstraint c1 = greaterThanOrEqual(1000).meet(lessThanOrEqual(100000));
        IntegralConstraint c2 = lessThan(0);

        IntegralConstraint c3 = c1.meet(c2);
        assertThat(c3).isSameAs(BOTTOM);
    }
}
