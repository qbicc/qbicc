package cc.quarkus.qcc.graph2;

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
        ;
    }
}
