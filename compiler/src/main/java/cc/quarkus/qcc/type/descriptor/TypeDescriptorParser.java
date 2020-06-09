package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.universe.Universe;

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

    public Type parseType() {
        while ( la() != (char) -1 ) {
            switch ( la() ) {
                case 'Z':
                    consume();
                    return Type.BOOL;
                case 'B':
                    consume();
                    return Type.S8;
                case 'C':
                    consume();
                    return Type.U16;
                case 'S':
                    consume();
                    return Type.S16;
                case 'I':
                    consume();
                    return Type.S32;
                case 'J':
                    consume();
                    return Type.S64;
                case 'F':
                    consume();
                    return Type.F32;
                case 'D':
                    consume();
                    return Type.F64;
                case 'V':
                    return Type.VOID;
                case 'L':
                    return parseClass();
                case '[':
                    consume();
                    return Type.arrayOf(parseType());
                default:
                    throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
            }
        }
        throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
    }

    protected Type parseClass() {
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
        // TODO: cache
        // TODO: the Type depends on the class loader
        return universe.findClass(name.toString()).getType();
    }

    private final Universe universe;
    protected final String descriptor;
    protected int cur;

}
