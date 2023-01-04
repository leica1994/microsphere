/**
 * Confucius commons project
 */
package io.github.microsphere.commons.util;

import io.github.microsphere.commons.constants.Constants;
import io.github.microsphere.commons.constants.FileSuffixConstants;
import io.github.microsphere.commons.constants.PathConstants;
import io.github.microsphere.commons.filter.ClassFileJarEntryFilter;
import io.github.microsphere.commons.io.FileUtils;
import io.github.microsphere.commons.io.scanner.SimpleFileScanner;
import io.github.microsphere.commons.io.scanner.SimpleJarEntryScanner;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * {@link Class} utility class
 *
 * @author <a href="mercyblitz@gmail.com">Mercy<a/>
 * @version 1.0.0
 * @see ClassUtils
 * @since 1.0.0
 */
public abstract class ClassUtils {

    private static final Map<String, Set<String>> classPathToClassNamesMap = initClassPathToClassNamesMap();

    private static final Map<String, String> classNameToClassPathsMap = initClassNameToClassPathsMap();

    private static final Map<String, Set<String>> packageNameToClassNamesMap = initPackageNameToClassNamesMap();

    private ClassUtils() {

    }

    private static Map<String, Set<String>> initClassPathToClassNamesMap() {
        Map<String, Set<String>> classPathToClassNamesMap = new LinkedHashMap<>();
        Set<String> classPaths = new LinkedHashSet<>();
        classPaths.addAll(ClassPathUtils.getBootstrapClassPaths());
        classPaths.addAll(ClassPathUtils.getClassPaths());
        for (String classPath : classPaths) {
            Set<String> classNames = findClassNamesInClassPath(classPath, true);
            classPathToClassNamesMap.put(classPath, classNames);
        }
        return Collections.unmodifiableMap(classPathToClassNamesMap);
    }

    private static Map<String, String> initClassNameToClassPathsMap() {
        Map<String, String> classNameToClassPathsMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : classPathToClassNamesMap.entrySet()) {
            String classPath = entry.getKey();
            Set<String> classNames = entry.getValue();
            for (String className : classNames) {
                classNameToClassPathsMap.put(className, classPath);
            }
        }

        return Collections.unmodifiableMap(classNameToClassPathsMap);
    }

    private static Map<String, Set<String>> initPackageNameToClassNamesMap() {
        Map<String, Set<String>> packageNameToClassNamesMap = new LinkedHashMap();
        for (Map.Entry<String, String> entry : classNameToClassPathsMap.entrySet()) {
            String className = entry.getKey();
            String packageName = resolvePackageName(className);
            Set<String> classNamesInPackage = packageNameToClassNamesMap.get(packageName);
            if (classNamesInPackage == null) {
                classNamesInPackage = new LinkedHashSet();
                packageNameToClassNamesMap.put(packageName, classNamesInPackage);
            }
            classNamesInPackage.add(className);
        }

        return Collections.unmodifiableMap(packageNameToClassNamesMap);
    }

    /**
     * Get all package names in {@link ClassPathUtils#getClassPaths() class paths}
     *
     * @return all package names in class paths
     */
    @Nonnull
    public static Set<String> getAllPackageNamesInClassPaths() {
        return packageNameToClassNamesMap.keySet();
    }

    /**
     * Resolve package name under specified class name
     *
     * @param className class name
     * @return package name
     */
    @Nullable
    public static String resolvePackageName(String className) {
        return StringUtils.substringBeforeLast(className, ".");
    }


    /**
     * Find all class names in class path
     *
     * @param classPath class path
     * @param recursive is recursive on sub directories
     * @return all class names in class path
     */
    @Nonnull
    public static Set<String> findClassNamesInClassPath(String classPath, boolean recursive) {
        File classesFileHolder = new File(classPath); // JarFile or Directory
        if (classesFileHolder.isDirectory()) { //Directory
            return findClassNamesInDirectory(classesFileHolder, recursive);
        } else if (classesFileHolder.isFile() && classPath.endsWith(FileSuffixConstants.JAR)) { //JarFile
            return findClassNamesInJarFile(classesFileHolder, recursive);
        }
        return Collections.emptySet();
    }

    /**
     * Find class path under specified class name
     *
     * @param type class
     * @return class path
     */
    @Nullable
    public static String findClassPath(Class<?> type) {
        return findClassPath(type.getName());
    }

    /**
     * Find class path under specified class name
     *
     * @param className class name
     * @return class path
     */
    @Nullable
    public static String findClassPath(String className) {
        return classNameToClassPathsMap.get(className);
    }

    /**
     * Gets class name {@link Set} under specified class path
     *
     * @param classPath class path
     * @param recursive is recursive on sub directories
     * @return non-null {@link Set}
     */
    @Nonnull
    public static Set<String> getClassNamesInClassPath(String classPath, boolean recursive) {
        Set<String> classNames = classPathToClassNamesMap.get(classPath);
        if (CollectionUtils.isEmpty(classNames)) {
            classNames = findClassNamesInClassPath(classPath, recursive);
        }
        return classNames;
    }

    /**
     * Gets class name {@link Set} under specified package
     *
     * @param onePackage one package
     * @return non-null {@link Set}
     */
    @Nonnull
    public static Set<String> getClassNamesInPackage(Package onePackage) {
        return getClassNamesInPackage(onePackage.getName());
    }

    /**
     * Gets class name {@link Set} under specified package name
     *
     * @param packageName package name
     * @return non-null {@link Set}
     */
    @Nonnull
    public static Set<String> getClassNamesInPackage(String packageName) {
        Set<String> classNames = packageNameToClassNamesMap.get(packageName);
        return classNames == null ? Collections.emptySet() : classNames;
    }


    protected static Set<String> findClassNamesInDirectory(File classesDirectory, boolean recursive) {
        Set<String> classNames = new LinkedHashSet();
        SimpleFileScanner simpleFileScanner = SimpleFileScanner.INSTANCE;
        Set<File> classFiles = simpleFileScanner.scan(classesDirectory, recursive, new SuffixFileFilter(FileSuffixConstants.CLASS));
        for (File classFile : classFiles) {
            String className = resolveClassName(classesDirectory, classFile);
            classNames.add(className);
        }
        return classNames;
    }

    protected static Set<String> findClassNamesInJarFile(File jarFile, boolean recursive) {
        if (!jarFile.exists()) {
            return Collections.emptySet();
        }

        Set<String> classNames = new LinkedHashSet();

        SimpleJarEntryScanner simpleJarEntryScanner = SimpleJarEntryScanner.INSTANCE;
        try {
            JarFile jarFile_ = new JarFile(jarFile);
            Set<JarEntry> jarEntries = simpleJarEntryScanner.scan(jarFile_, recursive, ClassFileJarEntryFilter.INSTANCE);

            for (JarEntry jarEntry : jarEntries) {
                String jarEntryName = jarEntry.getName();
                String className = resolveClassName(jarEntryName);
                if (StringUtils.isNotBlank(className)) {
                    classNames.add(className);
                }
            }

        } catch (Exception e) {

        }

        return classNames;
    }


    protected static String resolveClassName(File classesDirectory, File classFile) {
        String classFileRelativePath = FileUtils.resolveRelativePath(classesDirectory, classFile);
        return resolveClassName(classFileRelativePath);
    }

    /**
     * Resolve resource name to class name
     *
     * @param resourceName resource name
     * @return class name
     */
    public static String resolveClassName(String resourceName) {
        String className = StringUtils.replace(resourceName, PathConstants.SLASH, Constants.DOT);
        className = StringUtils.substringBefore(className, FileSuffixConstants.CLASS);
        while (StringUtils.startsWith(className, Constants.DOT)) {
            className = StringUtils.substringAfter(className, Constants.DOT);
        }
        return className;
    }

    /**
     * The map of all class names in {@link ClassPathUtils#getClassPaths() class path} , the class path for one {@link
     * JarFile} or classes directory as key , the class names set as value
     *
     * @return Read-only
     */
    @Nonnull
    public static Map<String, Set<String>> getClassPathToClassNamesMap() {
        return classPathToClassNamesMap;
    }

    /**
     * The set of all class names in {@link ClassPathUtils#getClassPaths() class path}
     *
     * @return Read-only
     */
    @Nonnull
    public static Set<String> getAllClassNamesInClassPaths() {
        Set<String> allClassNames = new LinkedHashSet();
        for (Set<String> classNames : classPathToClassNamesMap.values()) {
            allClassNames.addAll(classNames);
        }
        return Collections.unmodifiableSet(allClassNames);
    }


    /**
     * Get {@link Class}'s code source location URL
     *
     * @param type
     * @return If , return <code>null</code>.
     * @throws NullPointerException If <code>type</code> is <code>null</code> , {@link NullPointerException} will be thrown.
     */
    public static URL getCodeSourceLocation(Class<?> type) throws NullPointerException {

        URL codeSourceLocation = null;
        ClassLoader classLoader = type.getClassLoader();

        if (classLoader == null) { // Bootstrap ClassLoader or type is primitive or void
            String path = findClassPath(type);
            if (StringUtils.isNotBlank(path)) {
                try {
                    codeSourceLocation = new File(path).toURI().toURL();
                } catch (MalformedURLException ignored) {
                    codeSourceLocation = null;
                }
            }
        } else {
            ProtectionDomain protectionDomain = type.getProtectionDomain();
            CodeSource codeSource = protectionDomain == null ? null : protectionDomain.getCodeSource();
            codeSourceLocation = codeSource == null ? null : codeSource.getLocation();
        }
        return codeSourceLocation;
    }


}
