package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.Hidden;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.*;
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
    public static Class<?> get_or_create_array_class(Class<?> componentClass, uint8_t dimensions) {
        Class<?> arrayClass = CompilerIntrinsics.get_array_class_of(componentClass);
        if (arrayClass == null) {
            String className;
            type_id componentTypeId = CompilerIntrinsics.get_type_id_from_class(componentClass);
            if (CompilerIntrinsics.is_reference_array(componentTypeId) || CompilerIntrinsics.is_prim_array(componentTypeId)) {
                className = "[" + componentClass.getName();
                arrayClass = CompilerIntrinsics.create_class(className, CompilerIntrinsics.get_reference_array_typeid(), dimensions);
            } else {
                className = "[L" + componentClass.getName() + ";";
                arrayClass = CompilerIntrinsics.create_class(className, CompilerIntrinsics.get_reference_array_typeid(), dimensions);
            }
            if (!CompilerIntrinsics.set_array_class(componentClass, arrayClass)) {
                arrayClass = CompilerIntrinsics.get_array_class_of(componentClass);
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
    public static Class<?> get_array_class_of_dimension(Class<?> leafClass, uint8_t dimensions) {
        if (dimensions.intValue() == 1) {
            return get_or_create_array_class(leafClass, dimensions);
        }
        Class<?> cls = get_array_class_of_dimension(leafClass, CNative.word(dimensions.intValue()-1));
        return get_or_create_array_class(cls, dimensions);
    }

    /**
     * Checks if the java.lang.Class instance represents reference array class.
     * Reference array class have dimension greater than 0.
     *
     * @param cls
     * @return boolean
     */
    @Hidden
    public static boolean is_reference_array_class(Class<?> cls) {
        type_id typeId = CompilerIntrinsics.get_type_id_from_class(cls);
        return CompilerIntrinsics.is_reference_array(typeId);
    }

    /**
     * Creates java.lang.Class instance for array class
     *
     * @param leafClass leaf element of the array
     * @param dimensions dimensions of the array
     * @return instance of java.lang.Class
     */
    @Hidden
    public static Class<?> get_or_create_class_for_refarray(Class<?> leafClass, uint8_t dimensions) {
        return get_array_class_of_dimension(leafClass, dimensions);
    }

    static final int Flag_typeid_has_clinit = 1;
    static final int Flag_typeid_declares_default_methods = 2;
    static final int Flag_typeid_has_default_methods = 4;

    @Hidden
    public static boolean has_class_initializer(type_id typeId) {
        return (CompilerIntrinsics.get_typeid_flags(typeId) & Flag_typeid_has_clinit) == Flag_typeid_has_clinit;
    }

    @Hidden
    public static boolean declares_default_methods(type_id typeId) {
        return (CompilerIntrinsics.get_typeid_flags(typeId) & Flag_typeid_declares_default_methods) == Flag_typeid_declares_default_methods;
    }

    @Hidden
    public static boolean has_default_methods(type_id typeId) {
        return (CompilerIntrinsics.get_typeid_flags(typeId) & Flag_typeid_has_default_methods) == Flag_typeid_has_default_methods;
    }
}
