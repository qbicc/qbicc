package cc.quarkus.qcc;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.Universe;
import org.junit.Before;

public class AbstractTestCase {

    @Before
    public void setUpUniverse() {
        this.universe = new Universe( new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
    }

    protected String thisClassName() {
        return getClass().getName();
    }

    protected TypeDefinition getTypeDefinition() {
        return this.universe.findClass( thisClassName().replace('.', '/'));
    }

    protected TypeDefinition getTypeDefinition(Class<?> hostClass) {
        return this.universe.findClass( hostClass.getName().replace('.', '/'));
    }

    protected Universe universe;
}
