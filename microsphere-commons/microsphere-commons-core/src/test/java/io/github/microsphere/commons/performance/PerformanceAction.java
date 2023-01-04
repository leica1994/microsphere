package io.github.microsphere.commons.performance;

/**
 * {@link PerformanceAction}
 *
 * @author <a href="mercyblitz@gmail.com">Mercy<a/>
 * @version 1.0.0
 * @see PerformanceAction
 * @since 1.0.0
 */
public interface PerformanceAction<T> {

    T execute();

}
