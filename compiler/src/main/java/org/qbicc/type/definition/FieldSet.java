package org.qbicc.type.definition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.type.definition.element.FieldElement;

public final class FieldSet {
    final Map<String, Integer> fieldIndices = new HashMap<>();
    final FieldElement[] sortedFields;

    public FieldSet(ValidatedTypeDefinition type, boolean statics) {
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

    public int getIndex(String name) {
        Integer index = fieldIndices.get(name);

        if (index == null) {
            throw new IllegalArgumentException("No such field: " + name);
        }

        return index.intValue();
    }

    public FieldElement getField(String name) {
        return sortedFields[getIndex(name)];
    }

    public int getSize() {
        return sortedFields.length;
    }
}
