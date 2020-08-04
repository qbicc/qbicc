package cc.quarkus.qcc.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.DefinedFieldDefinition;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

final class FieldSet {
    final Map<String, Integer> fieldIndices = new HashMap<>();
    final DefinedFieldDefinition[] sortedFields;

    FieldSet(VerifiedTypeDefinition type, boolean statics) {
        int cnt = type.getFieldCount();
        List<DefinedFieldDefinition> fields = new ArrayList<>(cnt);
        type.eachField((field) -> {
            if (statics == field.isStatic()) {
                fields.add(field);
            }
        });
        fields.sort(Comparator.comparing(DefinedFieldDefinition::getName));

        sortedFields = fields.toArray((i) -> new DefinedFieldDefinition[i]);

        for (int i = 0; i < cnt; i++) {
            DefinedFieldDefinition field = sortedFields[i];
            fieldIndices.put(field.getName(), i);
        }
    }

    int getIndex(String name) {
        Integer index = fieldIndices.get(name);

        if (index == null) {
            throw new IllegalArgumentException("No such field: " + name);
        }

        return index;
    }

    DefinedFieldDefinition getType(String name) {
        return sortedFields[getIndex(name)];
    }

    int getSize() {
        return sortedFields.length;
    }
}
