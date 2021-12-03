package org.qbicc.runtime.main;

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
    public static Class<?> getOrCreateArrayClass(Class<?> componentClass, uint8_t dimensions) {
        Class<?> arrayClass = CompilerIntrinsics.getArrayClassOf(componentClass);
        if (arrayClass == null) {
            String className;
            type_id componentTypeId = CompilerIntrinsics.getTypeIdFromClass(componentClass);
            if (CompilerIntrinsics.isReferenceArray(componentTypeId) || CompilerIntrinsics.isPrimArray(componentTypeId)) {
                className = "[" + componentClass.getName();
                arrayClass = CompilerIntrinsics.createClass(className, CompilerIntrinsics.getReferenceArrayTypeId(), dimensions);
            } else {
                className = "[L" + componentClass.getName() + ";";
                arrayClass = CompilerIntrinsics.createClass(className, CompilerIntrinsics.getReferenceArrayTypeId(), dimensions);
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
            return getOrCreateArrayClass(leafClass, dimensions);
        }
        Class<?> cls = getArrayClassOfDimension(leafClass, CNative.word(dimensions.intValue()-1));
        return getOrCreateArrayClass(cls, dimensions);
    }

    /**
     * Checks if the java.lang.Class instance represents reference array class.
     * Reference array class have dimension greater than 0.
     *
     * @param cls
     * @return boolean
     */
    @Hidden
    public static boolean isReferenceArrayClass(Class<?> cls) {
        type_id typeId = CompilerIntrinsics.getTypeIdFromClass(cls);
        return CompilerIntrinsics.isReferenceArray(typeId);
    }

    /**
     * Creates java.lang.Class instance for array class
     *
     * @param leafClass leaf element of the array
     * @param dimensions dimensions of the array
     * @return instance of java.lang.Class
     */
    @Hidden
    public static Class<?> getOrCreateClassForRefArray(Class<?> leafClass, uint8_t dimensions) {
        return getArrayClassOfDimension(leafClass, dimensions);
    }

    static final int Flag_typeid_has_clinit = 1;
    static final int Flag_typeid_declares_default_methods = 2;
    static final int Flag_typeid_has_default_methods = 4;

    @Hidden
    public static boolean hasClassInitializer(type_id typeId) {
        return (CompilerIntrinsics.getTypeIdFlags(typeId) & Flag_typeid_has_clinit) == Flag_typeid_has_clinit;
    }

    @Hidden
    public static boolean declaresDefaultMethods(type_id typeId) {
        return (CompilerIntrinsics.getTypeIdFlags(typeId) & Flag_typeid_declares_default_methods) == Flag_typeid_declares_default_methods;
    }

    @Hidden
    public static boolean hasDefaultMethods(type_id typeId) {
        return (CompilerIntrinsics.getTypeIdFlags(typeId) & Flag_typeid_has_default_methods) == Flag_typeid_has_default_methods;
    }

    public static native void_ptr threadWrapperNative(void_ptr threadParam);
}
