package cc.quarkus.qcc.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.quarkus.qcc.type.definition.DefinedFieldDefinition;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

final class FieldSet {
    private static final String[] NO_STRINGS = new String[0];
    final String[] sortedNames;

    FieldSet(VerifiedTypeDefinition type, boolean statics) {
        int cnt = type.getFieldCount();
        List<String> names = new ArrayList<>(cnt);
        for (int i = 0; i < cnt; i ++) {
            DefinedFieldDefinition field = type.getFieldDefinition(i);
            if (statics == field.isStatic()) {
                names.add(field.getName());
            }
        }
        names.sort(String::compareTo);
        sortedNames = names.toArray(NO_STRINGS);
    }

    int getIndex(String name) {
        int idx = Arrays.binarySearch(sortedNames, name);
        if (idx < 0) {
            throw new IllegalArgumentException("No such field \"" + name + "\"");
        }
        return idx;
    }

    int getSize() {
        return sortedNames.length;
    }
}
