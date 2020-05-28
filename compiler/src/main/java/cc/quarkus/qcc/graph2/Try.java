package cc.quarkus.qcc.graph2;

/**
 * A terminator which designates a catch block for exceptional execution.
 */
public interface Try extends Terminator {
    /**
     * Get the catch value which represents the caught exception.  Only valid from the catch handler.
     *
     * @return the catch value
     */
    Value getCatchValue();
    BasicBlock getCatchHandler();
    void setCatchHandler(BasicBlock catchHandler);
}
