/**
 * Confucius commons project
 */
package io.github.microsphere.commons.filter;

import io.github.microsphere.commons.util.ClassUtils;

/**
 * {@link PackageNameClassNameFilter}
 *
 * @author <a href="mercyblitz@gmail.com">Mercy<a/>
 * @version 1.0.0
 * @see PackageNameClassNameFilter
 * @since 1.0.0
 */
public class PackageNameClassNameFilter implements Filter<String> {

    private final String packageName;
    private final boolean includedSubPackages;
    private final String subPackageNamePrefix;

    /**
     * Constructor
     *
     * @param packageName         the name of package
     * @param includedSubPackages included sub-packages
     */
    public PackageNameClassNameFilter(String packageName, boolean includedSubPackages) {
        this.packageName = packageName;
        this.includedSubPackages = includedSubPackages;
        this.subPackageNamePrefix = includedSubPackages ? packageName + "." : null;
    }

    @Override
    public boolean accept(String className) {
        String packageName = ClassUtils.resolvePackageName(className);
        boolean accepted = packageName.equals(this.packageName);
        if (!accepted && includedSubPackages) {
            accepted = packageName.startsWith(subPackageNamePrefix);
        }
        return accepted;
    }
}
