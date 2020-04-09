package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
public enum BaseTypeSignature implements TypeSignature {
    BYTE('B'),
    SHORT('S'),
    INT('I'),
    LONG('J'),
    CHAR('C'),
    FLOAT('F'),
    DOUBLE('D'),
    BOOLEAN('Z'),
    ;
    private final char name;

    BaseTypeSignature(final char name) {
        this.name = name;
    }

    public String toString() {
        return String.valueOf(name);
    }

    public char getName() {
        return name;
    }

    public static BaseTypeSignature forCharacter(char c) {
        switch (c) {
            case 'B': return BYTE;
            case 'S': return SHORT;
            case 'I': return INT;
            case 'J': return LONG;
            case 'C': return CHAR;
            case 'F': return FLOAT;
            case 'D': return DOUBLE;
            case 'Z': return BOOLEAN;
            default: return null;
        }
    }
}
