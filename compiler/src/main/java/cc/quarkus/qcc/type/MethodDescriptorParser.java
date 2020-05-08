package cc.quarkus.qcc.type;

import java.util.ArrayList;
import java.util.List;

public class MethodDescriptorParser extends TypeDescriptorParser {

    public MethodDescriptorParser(Universe universe, TypeDefinition owner, String name, String descriptor, boolean isStatic) {
        super(universe, descriptor);
        this.owner = owner;
        this.name = name;
        this.isStatic = isStatic;
    }

    public MethodDescriptor parseMethodDescriptor() {
        if ( la() != '(') {
            throw new RuntimeException("Unable to parse: " + this.descriptor + " at " + this.cur );
        }
        consume(); // (

        List<TypeDescriptor<?>> parameters = parseParameters();
        TypeDescriptor<?> returnType = parseType();
        return new MethodDescriptorImpl(this.owner, this.name, parameters, returnType, descriptor, isStatic);
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

    public TypeDefinition getOwner() {
        return this.owner;
    }

    public boolean isStatic() {
        return this.isStatic;
    }


    private final TypeDefinition owner;
    private final String name;
    private final boolean isStatic;
}
