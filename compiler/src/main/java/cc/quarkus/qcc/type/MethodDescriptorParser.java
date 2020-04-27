package cc.quarkus.qcc.type;

import java.util.ArrayList;
import java.util.List;

public class MethodDescriptorParser {

    public MethodDescriptorParser(Universe universe, TypeDefinition owner, String name, String descriptor, boolean isStatic) {
        this.universe = universe;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.isStatic = isStatic;
        this.cur = 0;
    }

    public MethodDescriptor parseMethodDescriptor() {
        if ( la() != '(') {
            throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
        }
        consume(); // (

        List<TypeDescriptor<?>> parameters = parseParameters();
        TypeDescriptor<?> returnType = parseType();
        return new MethodDescriptorImpl(this.owner, this.name, parameters, returnType, isStatic);
    }

    public List<TypeDescriptor<?>> parseParameters() {
        List<TypeDescriptor<?>> result = new ArrayList<>();

        if ( ! this.isStatic ) {
            result.add(TypeDescriptor.of(this.owner));
        }

        LOOP:
        while ( la() != (char) -1) {
            switch (la()) {
                case ')':
                    consume();
                    break LOOP;
                default:
                    result.add( parseType() );
            }
        }

        return result;
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
                //case '[':
                    //consume();
                    //return new ArrayDef(parseType());
                default:
                    throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
            }
        }
        throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
    }

    public TypeDefinition getOwner() {
        return this.owner;
    }

    public boolean isStatic() {
        return this.isStatic;
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

    private final Universe universe;
    private final TypeDefinition owner;
    private final String name;
    private final String descriptor;
    private final boolean isStatic;

    private int cur;
}
