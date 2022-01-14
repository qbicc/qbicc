package org.qbicc.runtime.main;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.CNative;
import org.qbicc.runtime.Hidden;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 *
 * These APIs are primarily intended for use in methods of VMHelper
 * and are subject to change as the runtime object model evolves.
 */
@SuppressWarnings("unused")
public class ObjectModel {

    /**
     * Returns java.lang.Class instance representing the array class of a given component class
     *
     * @param componentClass
     * @return instance of java.lang.Class
     */
    @Hidden
    private static Class<?> getOrCreateArrayClass(Class<?> componentClass) {
        Class<?> arrayClass = CompilerIntrinsics.getArrayClassOf(componentClass);
        if (arrayClass == null) {
            String className;
            type_id elemTypeId = CompilerIntrinsics.getTypeIdFromClass(componentClass);
            int dims = CompilerIntrinsics.getDimensionsFromClass(componentClass).intValue();
            if (dims > 254) {
                throw new IllegalArgumentException();
            }
            if (dims > 0 || CompilerIntrinsics.isPrimArray(elemTypeId)) {
                className = "[" + componentClass.getName();
                arrayClass = CompilerIntrinsics.createClass(className, elemTypeId, word(dims + 1), componentClass);
            } else {
                className = "[L" + componentClass.getName() + ";";
                arrayClass = CompilerIntrinsics.createClass(className, elemTypeId, word(1), componentClass);
            }
            if (!CompilerIntrinsics.setArrayClass(componentClass, arrayClass)) {
                arrayClass = CompilerIntrinsics.getArrayClassOf(componentClass);
            }
        }
        return arrayClass;
    }

    /**
     * Helper method to create java.lang.Class instance for array class
     *
     * @param leafClass leaf element of the array
     * @param dimensions dimensions of the array
     * @return instance of java.lang.Class
     */
    @Hidden
    public static Class<?> getArrayClassOfDimension(Class<?> leafClass, uint8_t dimensions) {
        if (dimensions.intValue() == 1) {
            return getOrCreateArrayClass(leafClass);
        }
        Class<?> cls = getArrayClassOfDimension(leafClass, CNative.word(dimensions.intValue()-1));
        return getOrCreateArrayClass(cls);
    }

    /**
     * Creates java.lang.Class instance for array class
     *
     * @param leafClass leaf element of the array
     * @param dimensions dimensions of the array
     * @return instance of java.lang.Class
     */
    @Hidden
    @AutoQueued
    public static Class<?> getOrCreateClassForRefArray(Class<?> leafClass, uint8_t dimensions) {
        return getArrayClassOfDimension(leafClass, dimensions);
    }
}
