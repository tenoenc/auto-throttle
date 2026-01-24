package io.github.tenoenc.autothrottle.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auto-throttle")
public class AutoThrottleProperties {

    /**
     * 리미터 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 윈도우 갱신 주기 (기본값: 100ms)
     * 단위: milliseconds
     */
    private long windowSizeMs = 100;

    /**
     * Vegas 알고리즘 Alpha 값 (하한선)
     */
    private int alpha = 3;

    /**
     * Vegas 알고리즘 Beta 값 (상한선)
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
