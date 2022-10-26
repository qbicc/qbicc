package org.qbicc.tests.snippets;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class ResourceLoading {
    static final Properties p = loadProps("test.properties");
    static final Properties q = loadPropsURL("test2.properties");
    static final Properties r = loadAllPropsURL("test.properties");

    public static void main(String[] args) {
        String first = p.getProperty("my.value.first", "0");
        String second = p.getProperty("my.value.second", "0");
        int sum = Integer.parseInt(first) + Integer.parseInt(second);
        check(sum == 11);
        check(q.getProperty("my.secret", "").equals("xyzzy"));
        int v1 = Integer.parseInt(r.getProperty("my.value.first", "0"));
        int v2 = Integer.parseInt(r.getProperty("my.value.second", "0"));
        check (v1 + v2 == 11);
    }

    static Properties loadProps(String name) {
        Properties prop = new Properties();
        try {
            prop.load(ResourceLoading.class.getClassLoader().getResourceAsStream(name));
        } catch (IOException e) {
        }
        return prop;
    }

    static Properties loadPropsURL(String name) {
        Properties prop = new Properties();
        try {
            URL pf = ResourceLoading.class.getClassLoader().getResource(name);
            prop.load(pf.openStream());
        } catch (IOException e) {
        }
        return prop;
    }

    static Properties loadAllPropsURL(String name) {
        Properties prop = new Properties();
        try {
            Enumeration<URL> pfs = ResourceLoading.class.getClassLoader().getResources(name);
            while (pfs.hasMoreElements()) {
                Properties tmp = new Properties();
                tmp.load(pfs.nextElement().openStream());
                prop.putAll(tmp);
            }
        } catch (IOException e) {
        }
        return prop;
    }

    static void check(boolean x) {
        if (x) {
            System.out.print("P");
        } else {
            System.out.print("F");
        }
    }
}
