package cc.quarkus.qcc.spi;

import java.io.IOException;
import java.io.InputStream;

public interface ClassFinder {
    InputStream findClass(String name) throws ClassNotFoundException, IOException;
}
