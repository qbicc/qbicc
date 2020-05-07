package cc.quarkus.qcc.type;

public class TypeDescriptorParser {
    public TypeDescriptorParser(Universe universe, String descriptor) {
        this.universe = universe;
        this.descriptor = descriptor;
        this.cur = 0;
    }

    protected String getDescriptor() {
        return this.descriptor;
    }

    protected char la(int num) {
        if ( ( this.cur + num ) > this.descriptor.length() ) {
            return (char) -1;
        }
        return descriptor.charAt( this.cur + num - 1);
    }

    protected char la() {
        return la(1);
    }

    protected char consume() {
        char c = la();
        ++this.cur;
        return c;
    }

    public TypeDescriptor<?> parseType() {
        while ( la() != (char) -1 ) {
            switch ( la() ) {
                case 'Z':
                    consume();
                    return TypeDescriptor.BOOLEAN;
                case 'B':
                    consume();
                    return TypeDescriptor.BYTE;
                case 'C':
                    consume();
                    return TypeDescriptor.CHAR;
                case 'S':
                    consume();
                    return TypeDescriptor.SHORT;
                case 'I':
                    consume();
                    return TypeDescriptor.INT;
                case 'J':
                    consume();
                    return TypeDescriptor.LONG;
                case 'F':
                    consume();
                    return TypeDescriptor.FLOAT;
                case 'D':
                    consume();
                    return TypeDescriptor.DOUBLE;
                case 'V':
                    return TypeDescriptor.VOID;
                case 'L':
                    return parseClass();
                case '[':
                    consume();
                    return parseType().array(1);
                default:
                    throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
            }
        }
        throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
    }

    protected TypeDescriptor<?> parseClass() {
        consume(); // L
        StringBuilder name = new StringBuilder();
        LOOP:
        while ( la() != (char) -1) {
            switch ( la() ) {
                case ';':
                    consume();
                    break LOOP;
                default:
                    name.append(consume());
            }
        }
        return TypeDescriptor.of(this.universe.findClass(name.toString()));
    }

    private final Universe universe;
    protected final String descriptor;
    protected int cur;

}
