/**
 * Confucius commons project
 */
package io.github.microsphere.commons.io.scanner;

import io.github.microsphere.commons.filter.FilterUtils;
import io.github.microsphere.commons.filter.PackageNameClassNameFilter;
import io.github.microsphere.commons.util.ClassLoaderUtils;
import io.github.microsphere.commons.util.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Simple {@link Class} Scanner
 *
 * @author <a href="mercyblitz@gmail.com">Mercy<a/>
 * @version 1.0.0
 * @see SimpleClassScanner
 * @since 1.0.0
 */
public class SimpleClassScanner {

    /**
     * Singleton
     */
    public final static SimpleClassScanner INSTANCE = new SimpleClassScanner();

    public SimpleClassScanner() {
    }

    /**
     * It's equal to invoke {@link #scan(ClassLoader, String, boolean, boolean)} method with
     * <code>requiredLoad=false</code> and <code>recursive=false</code>
     *
     * @param classLoader {@link ClassLoader}
     * @param packageName the name of package
     * @return {@link #scan(ClassLoader, String, boolean, boolean)} method with <code>requiredLoad=false</code>
     * @throws IllegalArgumentException scanned source is not legal
     * @throws IllegalStateException    scanned source's state is not valid
     */
    public Set<Class<?>> scan(ClassLoader classLoader, String packageName) throws IllegalArgumentException, IllegalStateException {
        return scan(classLoader, packageName, false);
    }

    /**
     * It's equal to invoke {@link #scan(ClassLoader, String, boolean, boolean)} method with
     * <code>requiredLoad=false</code>
     *
     * @param classLoader {@link ClassLoader}
     * @param packageName the name of package
     * @param recursive   included sub-package
     * @return {@link #scan(ClassLoader, String, boolean, boolean)} method with <code>requiredLoad=false</code>
     * @throws IllegalArgumentException scanned source is not legal
     * @throws IllegalStateException    scanned source's state is not valid
     */
    public Set<Class<?>> scan(ClassLoader classLoader, String packageName, boolean recursive) throws IllegalArgumentException, IllegalStateException {
        return scan(classLoader, packageName, recursive, false);
    }


    /**
     * scan {@link Class} set under specified package name or its' sub-packages in {@link ClassLoader}, if
     * <code>requiredLoad</code> indicates <code>true</code> , try to load those classes.
     *
     * @param classLoader  {@link ClassLoader}
     * @param packageName  the name of package
     * @param recursive    included sub-package
     * @param requiredLoad try to load those classes or not
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    public Set<Class<?>> scan(ClassLoader classLoader, String packageName, final boolean recursive, boolean requiredLoad) throws IllegalArgumentException, IllegalStateException {
        Set<Class<?>> classesSet = new LinkedHashSet();

        final String packageResourceName = ClassLoaderUtils.ResourceType.PACKAGE.resolve(packageName);

        try {
            Set<String> classNames = new LinkedHashSet();
            // Find in class loader
            Set<URL> resourceURLs = ClassLoaderUtils.getResources(classLoader, ClassLoaderUtils.ResourceType.PACKAGE, packageName);

            if (resourceURLs.isEmpty()) {
                //Find in class path
                List<String> classNamesInPackage = new ArrayList<>(ClassUtils.getClassNamesInPackage(packageName));

                if (!classNamesInPackage.isEmpty()) {
                    String classPath = ClassUtils.findClassPath(classNamesInPackage.get(0));
                    URL resourceURL = new File(classPath).toURI().toURL();
                    resourceURLs = new HashSet();
                    resourceURLs.add(resourceURL);
                }
            }

            for (URL resourceURL : resourceURLs) {
                URL classPathURL = resolveClassPathURL(resourceURL, packageResourceName);
                String classPath = classPathURL.getFile();
                Set<String> classNamesInClassPath = ClassUtils.findClassNamesInClassPath(classPath, true);
                classNames.addAll(filterClassNames(classNamesInClassPath, packageName, recursive));
            }

            for (String className : classNames) {
                Class<?> class_ = requiredLoad ? ClassLoaderUtils.loadClass(classLoader, className) : ClassLoaderUtils.findLoadedClass(classLoader, className);
                if (class_ != null) {
                    classesSet.add(class_);
                }
            }

        } catch (IOException e) {

        }
        return Collections.unmodifiableSet(classesSet);
    }

    private Set<String> filterClassNames(Set<String> classNames, String packageName, boolean recursive) {
        PackageNameClassNameFilter packageNameClassNameFilter = new PackageNameClassNameFilter(packageName, recursive);
        Set<String> filterClassNames = new LinkedHashSet(FilterUtils.filter(classNames, packageNameClassNameFilter));
        return filterClassNames;
    }


    private URL resolveClassPathURL(URL resourceURL, String packageResourceName) {
        String resource = resourceURL.toExternalForm();
        String classPath = StringUtils.substringBefore(resource, packageResourceName);
        URL classPathURL = null;
        try {
            classPathURL = new URL(classPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return classPathURL;
    }


}
