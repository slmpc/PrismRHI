package com.github.slmpc.prismrhi.context;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RhiContextCreateInfo(
        RhiContextMode mode,
        String title,
        int width,
        int height,
        boolean resizable,
        boolean visible,
        long windowHandle,
        long surfaceHandle,
        boolean ownsWindow,
        boolean ownsSurface,
        Set<String> requiredInstanceExtensions
) {
    public RhiContextCreateInfo {
        mode = Objects.requireNonNull(mode, "mode");
        title = title == null || title.isBlank() ? "PrismRHI" : title;
        width = width <= 0 ? 1280 : width;
        height = height <= 0 ? 720 : height;
        requiredInstanceExtensions = Set.copyOf(requiredInstanceExtensions == null ? Set.of() : requiredInstanceExtensions);
        if (mode == RhiContextMode.GLFW_WINDOW && windowHandle == 0L) {
            throw new IllegalArgumentException("GLFW_WINDOW context requires a window handle");
        }
        if ((mode == RhiContextMode.EXTERNAL_SURFACE || mode == RhiContextMode.EXTERNAL_VULKAN_SURFACE)
                && surfaceHandle == 0L) {
            throw new IllegalArgumentException("EXTERNAL_SURFACE context requires a surface handle");
        }
    }

    public static Builder autoGlfwWindow(int width, int height, String title) {
        return new Builder(RhiContextMode.AUTO_GLFW_WINDOW)
                .size(width, height)
                .title(title)
                .ownsWindow(true)
                .ownsSurface(true);
    }

    public static Builder glfwWindow(long windowHandle, int width, int height) {
        return new Builder(RhiContextMode.GLFW_WINDOW)
                .windowHandle(windowHandle)
                .size(width, height)
                .ownsWindow(false)
                .ownsSurface(true);
    }

    public static Builder externalSurface(long surfaceHandle, int width, int height) {
        return new Builder(RhiContextMode.EXTERNAL_SURFACE)
                .surfaceHandle(surfaceHandle)
                .size(width, height)
                .ownsWindow(false)
                .ownsSurface(false);
    }

    public static Builder externalVulkanSurface(long surfaceHandle, int width, int height) {
        return new Builder(RhiContextMode.EXTERNAL_VULKAN_SURFACE)
                .surfaceHandle(surfaceHandle)
                .size(width, height)
                .requiredInstanceExtension("VK_KHR_surface")
                .ownsWindow(false)
                .ownsSurface(false);
    }

    public boolean needsGlfwSurface() {
        return mode == RhiContextMode.AUTO_GLFW_WINDOW || mode == RhiContextMode.GLFW_WINDOW;
    }

    public static final class Builder {
        private final RhiContextMode mode;
        private String title = "PrismRHI";
        private int width = 1280;
        private int height = 720;
        private boolean resizable = true;
        private boolean visible = true;
        private long windowHandle;
        private long surfaceHandle;
        private boolean ownsWindow;
        private boolean ownsSurface;
        private final Set<String> requiredInstanceExtensions = new LinkedHashSet<>();

        private Builder(RhiContextMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Builder windowHandle(long windowHandle) {
            this.windowHandle = windowHandle;
            return this;
        }

        public Builder surfaceHandle(long surfaceHandle) {
            this.surfaceHandle = surfaceHandle;
            return this;
        }

        public Builder ownsWindow(boolean ownsWindow) {
            this.ownsWindow = ownsWindow;
            return this;
        }

        public Builder ownsSurface(boolean ownsSurface) {
            this.ownsSurface = ownsSurface;
            return this;
        }

        public Builder requiredInstanceExtension(String extension) {
            if (extension != null && !extension.isBlank()) {
                requiredInstanceExtensions.add(extension);
            }
            return this;
        }

        public Builder requiredInstanceExtensions(Iterable<String> extensions) {
            if (extensions != null) {
                for (String extension : extensions) {
                    requiredInstanceExtension(extension);
                }
            }
            return this;
        }

        public RhiContextCreateInfo build() {
            return new RhiContextCreateInfo(
                    mode,
                    title,
                    width,
                    height,
                    resizable,
                    visible,
                    windowHandle,
                    surfaceHandle,
                    ownsWindow,
                    ownsSurface,
                    requiredInstanceExtensions
            );
        }
    }
}
