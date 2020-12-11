package cc.quarkus.qcc.type.annotation.type;

import static cc.quarkus.qcc.type.annotation.type.TypeAnnotation.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 * A hierarchical type annotation list.
 */
public final class TypeAnnotationList implements Iterable<TypeAnnotation> {
    // enclosing path kind (if any)
    final TypePathKind pathKind;
    // enclosing path kind index (type argument)
    final int idx;
    final List<TypeAnnotationList> nested;
    final List<TypeAnnotation> annotations;

    TypeAnnotationList(final TypePathKind pathKind, final int idx, final List<TypeAnnotationList> nested, final List<TypeAnnotation> annotations) {
        this.pathKind = pathKind;
        this.idx = idx;
        this.nested = nested;
        this.annotations = annotations;
    }

    public TypeAnnotationList inArray() {
        for (TypeAnnotationList item : nested) {
            if (item.pathKind == TypePathKind.ARRAY) {
                return item;
            }
        }
        return empty();
    }

    public TypeAnnotationList inNestedType() {
        for (TypeAnnotationList item : nested) {
            if (item.pathKind == TypePathKind.NESTED) {
                return item;
            }
        }
        return empty();
    }

    public TypeAnnotationList onWildCardBound() {
        for (TypeAnnotationList item : nested) {
            if (item.pathKind == TypePathKind.WILDCARD_BOUND) {
                return item;
            }
        }
        return empty();
    }

    public TypeAnnotationList onTypeArgument(int idx) {
        for (TypeAnnotationList item : nested) {
            if (item.pathKind == TypePathKind.ARRAY && item.idx == idx) {
                return item;
            }
        }
        return empty();
    }

    /**
     * Iterate over the type annotations at this level.
     *
     * @return the type annotations at this level
     */
    public Iterator<TypeAnnotation> iterator() {
        return annotations.iterator();
    }

    private static final TypeAnnotationList EMPTY = new TypeAnnotationList(null, 0, List.of(), List.of());

    public static TypeAnnotationList empty() {
        return EMPTY;
    }

    private static final Predicate<TypeAnnotation> TOP = a -> true;

    public static TypeAnnotationList parse(final ClassFile classFile, final ClassContext ctxt, final ByteBuffer buf) {
        int cnt = nextShort(buf);
        if (cnt == 0) {
            return empty();
        }
        TypeAnnotation[] array = new TypeAnnotation[cnt];
        for (int i = 0; i < cnt; i ++) {
            array[i] = TypeAnnotation.parse(classFile, ctxt, buf);
        }
        return build(List.of(array));
    }

    public static TypeAnnotationList build(List<TypeAnnotation> rawAnnotationList) {
        if (rawAnnotationList.isEmpty()) {
            return empty();
        }
        TypeAnnotationList rootList = new TypeAnnotationList(null, 0, new ArrayList<>(3), new ArrayList<>(3));
        for (TypeAnnotation typeAnnotation : rawAnnotationList) {
            findOrCreateNested(rootList, typeAnnotation, 0).annotations.add(typeAnnotation);
        }
        return rootList;
    }

    private static TypeAnnotationList findOrCreateNested(TypeAnnotationList rootList, TypeAnnotation annotation, int idx) {
        if (idx == annotation.getPathLength()) {
            return rootList;
        }
        List<TypeAnnotationList> nested = rootList.nested;
        TypePathKind searchKind = annotation.getPathKind(idx);
        int searchIdx = annotation.getPathArgumentIndex(idx);
        for (TypeAnnotationList list : nested) {
            if (list.pathKind == searchKind && list.idx == searchIdx) {
                return findOrCreateNested(list, annotation, idx + 1);
            }
        }
        // have to create it
        TypeAnnotationList newList = findOrCreateNested(new TypeAnnotationList(searchKind, searchIdx, new ArrayList<>(3), new ArrayList<>(3)), annotation, idx + 1);
        rootList.nested.add(newList);
        return newList;
    }
}
