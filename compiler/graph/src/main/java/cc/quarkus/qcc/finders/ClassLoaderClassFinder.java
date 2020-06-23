package cc.quarkus.qcc.finders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import cc.quarkus.qcc.spi.ClassFinder;

public class ClassLoaderClassFinder implements ClassFinder {

    public ClassLoaderClassFinder(ClassLoader cl) {
        assert cl != null : "Null classloader not allowed";
        this.cl = cl;
    }

    @Override
    public InputStream findClass(String name) throws IOException {
        URL resource = this.cl.getResource(name.replace(".", "/") + ".class");
        if ( resource == null ) {
            throw new FileNotFoundException(name);
        }
        return resource.openStream();
    }

    private final ClassLoader cl;
}
