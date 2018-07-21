package vn.softdreams.springsaml.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by chen on 7/19/18.
 */
public class Utils {
    public static Reader getXml(String name) throws IOException {
        ClassLoader classLoader = Utils.class.getClassLoader();
        return new InputStreamReader(classLoader.getResourceAsStream(name), "UTF-8");
    }

    public static InputStream getP12(String name) throws Exception {
        ClassLoader classLoader = Utils.class.getClassLoader();
        return classLoader.getResourceAsStream(name);
    }

    public static boolean isNullOrEmpty(String str) {
        if (str == null) return false;
        return str.isEmpty();
    }
}
