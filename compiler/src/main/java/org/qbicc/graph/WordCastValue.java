package org.qbicc.graph;

import org.qbicc.type.WordType;

/**
 *
 */
public interface WordCastValue extends CastValue {
    WordType getType();

    WordType getInputType();

    default <W extends WordType> W getInputType(Class<W> type) {
        return getInput().getType(type);
    }
}
