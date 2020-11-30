package cc.quarkus.qcc.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import io.smallrye.common.constraint.Assert;

/**
 * A section in a program.
 */
public final class Section extends ProgramObject {
    final ProgramModule programModule;
    final List<ProgramObject> contents = Collections.synchronizedList(new ArrayList<>());

    Section(final String name, final SymbolLiteral literal, final ProgramModule programModule) {
        super(name, literal);
        this.programModule = programModule;
    }

    public Data addData(BasicElement originalElement, String name, Value value) {
        return add(new Data(
            originalElement, Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, Assert.checkNotNullParam("value", value).getType()),
            value
        ));
    }

    public Function addFunction(BasicElement originalElement, String name, FunctionType type) {
        return add(new Function(
            originalElement, Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, type)
        ));
    }

    private <T extends ProgramObject> T add(T item) {
        contents.add(item);
        return item;
    }

    public Iterable<ProgramObject> contents() {
        return List.of(contents.toArray(ProgramObject[]::new));
    }
}
