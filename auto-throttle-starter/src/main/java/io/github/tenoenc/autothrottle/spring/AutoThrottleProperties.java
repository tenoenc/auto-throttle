package io.github.tenoenc.autothrottle.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Auto Throttle.
 * <p>
 * These properties can be configured in {@code application.yml} or {@code application.properties}
 * under the prefix {@code auto-throttle}.
 * </p>
 */
@ConfigurationProperties(prefix = "auto-throttle")
public class AutoThrottleProperties {

    /**
     * Whether to enable the auto-throttle limiter.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * The time window for aggregating statistics and updating the limit.
     * <p>
     * Shorter windows react faster to changes but may be more volatile.
     * Longer windows are more stable but slower to react.
     * </p>
     * Unit: milliseconds. Default: 100ms.
     */
    private long windowSizeMs = 100;

    /**
     * The alpha parameter for the Vegas algorithm (Lower Bound).
     * Represents the minimum expected queue size.
     * If the estimated queue is smaller than this, the limit increases.
     */
    private int alpha = 3;

    /**
     * The beta parameter for the Vegas algorithm (Upper Bound).
     * Represents the maximum expected queue size.
     * If the estimated queue is larger than this, the limit decreases.
     */
    private int beta = 6;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getWindowSizeMs() { return windowSizeMs; }
    public void setWindowSizeMs(long windowSizeMs) { this.windowSizeMs = windowSizeMs; }

    public int getAlpha() { return alpha; }
    public void setAlpha(int alpha) { this.alpha = alpha; }

    public int getBeta() { return beta; }
    public void setBeta(int beta) { this.beta = beta; }
}