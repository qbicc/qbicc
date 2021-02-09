package cc.quarkus.qcc.type.descriptor;

import java.nio.ByteBuffer;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.ClassContext;

/**
 *
 */
public final class ClassTypeDescriptor extends TypeDescriptor {
    private final String packageName;
    private final String className;

    ClassTypeDescriptor(final String packageName, final String className) {
        super(Objects.hash(ClassTypeDescriptor.class, packageName, className));
        this.packageName = packageName;
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public boolean packageAndClassNameEquals(final String packageName, final String className) {
        return this.packageName.equals(packageName) && this.className.equals(className);
    }

    public boolean equals(final TypeDescriptor other) {
        return other instanceof ClassTypeDescriptor && equals((ClassTypeDescriptor) other);
    }

    public boolean equals(final ClassTypeDescriptor other) {
        return super.equals(other) && packageName.equals(other.packageName) && className.equals(other.className);
    }

    public StringBuilder toString(final StringBuilder target) {
        target.append('L');
        if (!packageName.isEmpty()) {
            target.append(packageName).append('/');
        }
        return target.append(className).append(';');
    }

    public static ClassTypeDescriptor synthesize(final ClassContext classContext, final String internalName) {
        int idx = internalName.lastIndexOf('/');
        String packageName;
        String className;
        if (idx == -1) {
            packageName = "";
            className = classContext.deduplicate(internalName);
        } else {
            packageName = classContext.deduplicate(internalName.substring(0, idx));
            className = classContext.deduplicate(internalName.substring(idx + 1));
        }
        return Cache.get(classContext).getClassTypeDescriptor(packageName, className);
    }

    public static ClassTypeDescriptor parseClassConstant(final ClassContext classContext, final ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();
        int i;
        for (;;) {
            i = buf.hasRemaining() ? peek(buf) : ';';
            if (i == ';') {
                // no package
                return Cache.get(classContext).getClassTypeDescriptor("", classContext.deduplicate(sb.toString()));
            } else if (i == '/') {
                int lastSlash = sb.length();
                buf.get(); // consume '/'
                sb.appendCodePoint('/');
                for (;;) {
                    i = buf.hasRemaining() ? peek(buf) : ';';
                    if (i == ';') {
                        return Cache.get(classContext).getClassTypeDescriptor(classContext.deduplicate(sb.substring(0, lastSlash)), classContext.deduplicate(sb.substring(lastSlash + 1)));
                    } else if (i == '/') {
                        lastSlash = sb.length();
                        buf.get(); // consume '/'
                        sb.appendCodePoint('/');
                    } else {
                        sb.appendCodePoint(codePoint(buf));
                    }
                }
            } else {
                sb.appendCodePoint(codePoint(buf));
            }
        }
    }

    public static ClassTypeDescriptor parse(final ClassContext classContext, final ByteBuffer buf) {
        int i = next(buf);
        if (i != 'L') {
            throw parseError();
        }
        ClassTypeDescriptor desc = parseClassConstant(classContext, buf);
        expect(buf, ';');
        return desc;
    }
}
