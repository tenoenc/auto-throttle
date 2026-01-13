package io.github.tenoenc.autothrottle.core;

/**
 * An abstraction for the time source.
 * <p>
 * This interface is used to decouple the system clock from the business logic,
 * allowing for:
 * <ul>
 * <li>Deterministically testing time-dependent logic (Time Travel).</li>
 * <li>Replacing the underlying time source if needed.</li>
 * </ul>
 * </p>
 */
@FunctionalInterface
public interface NanoClock {

    /**
     * Returns the current value of the running timer, in nanoseconds.
     *
     * @return The current value of the system timer.
     */
    long nanoTime();

    /**
     * Returns a default implementation that delegates to {@link System#nanoTime()}.
     *
     * @return A system-based NanoClock.
     */
    static NanoClock system() {
        return System::nanoTime;
    }
}