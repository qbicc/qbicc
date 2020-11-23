package cc.quarkus.qcc.object;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.FunctionType;
import io.smallrye.common.constraint.Assert;

/**
 * A section in a program.
 */
public final class Section extends ProgramObject {
    final ProgramModule programModule;
    final List<ProgramObject> contents = new ArrayList<>();

    Section(final String name, final SymbolLiteral literal, final ProgramModule programModule) {
        super(name, literal);
        this.programModule = programModule;
    }

    public Section add(SectionObject item) {
        Assert.checkNotNullParam("item", item);
        contents.add(item);
        return this;
    }

    public Data addData(String name, Value value) {
        return new Data(
            Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, Assert.checkNotNullParam("value", value).getType()),
            value
        );
    }

    public Function addFunction(String name, FunctionType type) {
        return new Function(
            Assert.checkNotNullParam("name", name),
            programModule.literalFactory.literalOfSymbol(name, type)
        );
    }
}
