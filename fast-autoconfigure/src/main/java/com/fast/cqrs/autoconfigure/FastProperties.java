package com.fast.cqrs.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Fast Framework.
 * <p>
 * Example configuration in application.yml:
 * <pre>{@code
 * fast:
 *   native:
 *     enabled: true  # Enable GraalVM native image optimizations
 *   aot:
 *     enabled: true  # Use AOT-generated code (recommended for native)
 * }</pre>
 */
@ConfigurationProperties(prefix = "fast")
public class FastProperties {

    private final Native nativeImage = new Native();
    private final Aot aot = new Aot();

    public Native getNative() {
        return nativeImage;
    }

    public Aot getAot() {
        return aot;
    }

    /**
     * GraalVM Native Image configuration.
     */
    public static class Native {
        
        /**
         * Enable GraalVM native image optimizations.
         * When enabled, registers runtime hints for reflection-based code.
         * Default: auto-detected based on environment.
         */
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns true if native image mode is enabled.
         * Auto-detects if running in native image when not explicitly set.
         */
        public boolean isEnabled() {
            if (enabled != null) {
                return enabled;
            }
            // Auto-detect: check if running in GraalVM native image
            return isRunningInNativeImage();
        }

        private boolean isRunningInNativeImage() {
            // GraalVM sets this property when running as native image
            String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");
            return imageCode != null;
        }
    }

    /**
     * AOT (Ahead-of-Time) compilation configuration.
     */
    public static class Aot {
        
        /**
         * Use AOT-generated implementations instead of reflection-based ones.
         * Recommended for GraalVM native images.
         * Default: true (prefers generated code)
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
