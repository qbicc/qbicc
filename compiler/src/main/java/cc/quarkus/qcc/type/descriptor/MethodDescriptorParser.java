package cc.quarkus.qcc.type.descriptor;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;

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

        List<Type> parameters = parseParameters();
        Type returnType = parseType();
        return new MethodDescriptorImpl(this.owner, this.name, parameters, returnType, descriptor, isStatic);
    }

    public List<Type> parseParameters() {
        List<Type> result = new ArrayList<>();

        //if ( ! this.isStatic ) {
            // receiver
            //result.add(this.owner.getType());
        //}

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
