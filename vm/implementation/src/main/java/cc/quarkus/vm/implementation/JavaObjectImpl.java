package cc.quarkus.vm.implementation;

import cc.quarkus.vm.api.JavaClass;
import cc.quarkus.vm.api.JavaObject;

class JavaObjectImpl implements JavaObject {
    final JavaClassImpl class_;
    final FieldContainer fields;

    JavaObjectImpl(final JavaClassImpl class_) {
        this.class_ = class_;
        fields = new FieldContainer(class_.definition, class_.instanceFields);
    }

    JavaObjectImpl(final FieldContainer fieldContainer) {
        // only called from JavaClassImpl
        this.class_ = (JavaClassImpl) this;
        fields = fieldContainer;
    }

    public JavaClass getJavaClass() {
        return class_;
    }
}
