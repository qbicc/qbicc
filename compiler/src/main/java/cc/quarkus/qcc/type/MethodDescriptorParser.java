package cc.quarkus.qcc.type;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.BooleanType;
import cc.quarkus.qcc.graph.type.ByteType;
import cc.quarkus.qcc.graph.type.CharType;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.DoubleType;
import cc.quarkus.qcc.graph.type.FloatType;
import cc.quarkus.qcc.graph.type.IntType;
import cc.quarkus.qcc.graph.type.LongType;
import cc.quarkus.qcc.graph.type.ObjectType;
import cc.quarkus.qcc.graph.type.ShortType;
import cc.quarkus.qcc.graph.type.VoidType;

public class MethodDescriptorParser {

    public MethodDescriptorParser(Universe universe, String descriptor, boolean isStatic, TypeDefinition owner) {
        this.universe = universe;
        this.descriptor = descriptor;
        this.isStatic = isStatic;
        this.owner = owner;
        this.cur = 0;
    }

    public MethodDescriptor parseMethodDescriptor() {
        if ( la() != '(') {
            throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
        }
        consume(); // (

        List<ConcreteType<?>> parameters = parseParameters();
        ConcreteType<?> returnType = parseType();
        return new MethodDescriptor(parameters, returnType);
    }

    public List<ConcreteType<?>> parseParameters() {
        List<ConcreteType<?>> result = new ArrayList<>();

        if ( ! this.isStatic ) {
            result.add(ObjectType.of(this.owner));
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

    public ConcreteType<?> parseType() {
        while ( la() != (char) -1 ) {
            switch ( la() ) {
                case 'Z':
                    consume();
                    return BooleanType.INSTANCE;
                case 'B':
                    consume();
                    return ByteType.INSTANCE;
                case 'C':
                    consume();
                    return CharType.INSTANCE;
                case 'S':
                    consume();
                    return ShortType.INSTANCE;
                case 'I':
                    consume();
                    return IntType.INSTANCE;
                case 'J':
                    consume();
                    return LongType.INSTANCE;
                case 'F':
                    consume();
                    return FloatType.INSTANCE;
                case 'D':
                    consume();
                    return DoubleType.INSTANCE;
                case 'V':
                    return VoidType.INSTANCE;
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

    protected ConcreteType<?> parseClass() {
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

        return this.universe.findType(name.toString());
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
    private final String descriptor;
    private final TypeDefinition owner;
    private final boolean isStatic;

    private int cur;
}
