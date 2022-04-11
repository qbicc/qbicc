package org.qbicc.object;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MemberElement;

/**
 * A declaration of some global data item.
 */
public class DataDeclaration extends Declaration {

    DataDeclaration(final MemberElement originalElement, ProgramModule programModule, final String name, final ValueType valueType) {
        super(originalElement, programModule, name, valueType);
    }

    DataDeclaration(final Data original) {
        super(original);
    }

    @Override
    public DataDeclaration getDeclaration() {
        return this;
    }
}
