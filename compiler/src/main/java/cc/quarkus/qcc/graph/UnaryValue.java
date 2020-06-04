package cc.quarkus.qcc.graph;

/**
 *
 */
public interface UnaryValue extends Value, ProgramNode {
    Value getInput();
    void setInput(Value input);
    Kind getKind();
    void setKind(Kind kind);

    enum Kind {
        NEGATE,
        LENGTH_OF,
        SIZE_OF,
        ;
    }
}
