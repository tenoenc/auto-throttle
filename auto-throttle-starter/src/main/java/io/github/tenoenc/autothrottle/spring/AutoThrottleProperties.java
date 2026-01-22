package io.github.tenoenc.autothrottle.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auto-throttle")
public class AutoThrottleProperties {

    private boolean enabled = true;
    private long windowSizeMs = 1000;
    private int alpha = 3;
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
