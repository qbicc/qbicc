package org.qbicc.plugin.stringpool;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.object.Data;

/**
 * Represents a pool of strings to be stored in the final object file.
 * Any string representing some sort of metadata can be put in this pool.
 * Each string is identified uniquely using {@link StringId}.
 */
public interface StringPool {
    AttachmentKey<StringPool> KEY = new AttachmentKey<>();

    static StringPool get(final CompilationContext context) {
        StringPool stringPool = context.getAttachment(KEY);
        if (stringPool == null) {
            StringPool appearing = context.putAttachmentIfAbsent(KEY, stringPool = new OffsetBasedStringPool());
            if (appearing != null) {
                stringPool = appearing;
            }
        }
        return stringPool;
    }

    /**
     * Add a string to the pool and return a unique identifier to it.
     * @param str
     * @return StringId
     */
    StringId add(String str);

    /**
     * Finds the string corresponding to the StringId
     * @param id
     * @return String corresponding to the StringId
     */
    String findString(StringId id);

    /**
     * Emit the string pool
     * @return
     */
    Data emit(CompilationContext context);
}
