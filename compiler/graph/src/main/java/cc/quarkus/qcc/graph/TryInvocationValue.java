package cc.quarkus.qcc.graph;

/**
 * An invoke instruction within a {@code try} block which returns a value.
 */
public interface TryInvocationValue extends Try, InvocationValue, Goto, GraphFactory.TerminatorValue {
    default Value getValue() {
        return this;
    }

    default Terminator getTerminator() {
        return this;
    }
}
