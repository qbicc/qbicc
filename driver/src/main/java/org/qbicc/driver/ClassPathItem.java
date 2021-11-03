package org.qbicc.driver;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 * An item on the class path which can be made up of one or more layered resource roots and zero or more layered source file
 * roots.  Typically there will be one class root and zero or one corresponding source root(s) per item.
 *
 * @param name the name of the root (must not be {@code null})
 * @param classRoots the class roots (must not be {@code null})
 * @param sourceRoots the source roots (must not be {@code null})
 */
public record ClassPathItem(String name, List<ClassPathElement> classRoots, List<ClassPathElement> sourceRoots) implements Closeable {
    public ClassPathItem {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("classRoots", classRoots);
        Assert.checkNotNullParam("sourceRoots", sourceRoots);
    }

    /**
     * Find a resource in this item.
     *
     * @param name the resource name (must not be {@code null})
     * @return the resource or {@link ClassPathElement#NON_EXISTENT} if no resource is found (not {@code null})
     * @throws IOException if an error occurs while loading the resource
     */
    public ClassPathElement.Resource findResource(String name) throws IOException {
        Assert.checkNotNullParam("name", name);
        for (ClassPathElement classRoot : classRoots) {
            ClassPathElement.Resource resource = classRoot.getResource(name);
            if (resource != ClassPathElement.NON_EXISTENT) {
                return resource;
            }
        }
        return ClassPathElement.NON_EXISTENT;
    }

    /**
     * Find a source file resource in this item.
     *
     * @param packagePath the package path (must not be {@code null} but may be empty)
     * @param className the class name (must not be {@code null})
     * @param fileName the source file name (must not be {@code null})
     * @return the resource corresponding to the source file, or {@link ClassPathElement#NON_EXISTENT} if it is not found
     * @throws IOException if an error occurs while loading the resource
     */
    public ClassPathElement.Resource findSourceFile(String packagePath, String className, String fileName) throws IOException {
        Assert.checkNotNullParam("packagePath", packagePath);
        Assert.checkNotNullParam("className", className);
        Assert.checkNotNullParam("fileName", fileName);
        ClassPathElement.Resource resource;
        for (ClassPathElement sourceRoot : sourceRoots) {
            String name;
            if (! packagePath.isEmpty()) {
                name = packagePath + "/" + fileName;
                resource = sourceRoot.getResource(name);
                if (resource != ClassPathElement.NON_EXISTENT) {
                    return resource;
                }
            }
            name = fileName;
            resource = sourceRoot.getResource(name);
            if (resource != ClassPathElement.NON_EXISTENT) {
                return resource;
            }
        }
        return ClassPathElement.NON_EXISTENT;
    }

    @Override
    public void close() {
        for (List<ClassPathElement> rootList : List.of(classRoots, sourceRoots)) {
            for (ClassPathElement classRoot : rootList) try {
                classRoot.close();
            } catch (IOException ignored) {
            }
        }
    }
}
