package cc.quarkus.qcc.type.definition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.type.definition.element.FieldElement;

public final class FieldSet {
    final Map<String, Integer> fieldIndices = new HashMap<>();
    final FieldElement[] sortedFields;

    public FieldSet(VerifiedTypeDefinition type, boolean statics) {
        int cnt = type.getFieldCount();
        List<FieldElement> fields = new ArrayList<>(cnt);
        type.eachField((field) -> {
            if (statics == field.isStatic()) {
                fields.add(field);
            }
        });
        fields.sort(Comparator.comparing(FieldElement::getName));

        sortedFields = fields.toArray(FieldElement[]::new);

        for (int i = 0; i < sortedFields.length; i++) {
            FieldElement field = sortedFields[i];
            fieldIndices.put(field.getName(), Integer.valueOf(i));
        }
    }

    int getIndex(String name) {
        Integer index = fieldIndices.get(name);

        if (index == null) {
            throw new IllegalArgumentException("No such field: " + name);
        }

        return index.intValue();
    }

    FieldElement getType(String name) {
        return sortedFields[getIndex(name)];
    }

    int getSize() {
        return sortedFields.length;
    }
}
