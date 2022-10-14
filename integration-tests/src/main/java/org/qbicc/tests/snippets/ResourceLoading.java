package org.qbicc.tests.snippets;

import java.io.IOException;
import java.util.Properties;

public class ResourceLoading {
    static final Properties p = loadProps("test.properties");

    public static void main(String[] args) {
        String first = p.getProperty("my.value.first");
        String second = p.getProperty("my.value.second");
        int sum = Integer.parseInt(first) + Integer.parseInt(second);
        if (sum == 11) {
            System.out.print("P");
        } else {
            System.out.print("F");
        }
    }

    static Properties loadProps(String name) {
        Properties prop = new Properties();
        try {
            prop.load(ResourceLoading.class.getClassLoader().getResourceAsStream(name));
        } catch (IOException e) {
        }
        return prop;
    }
}
