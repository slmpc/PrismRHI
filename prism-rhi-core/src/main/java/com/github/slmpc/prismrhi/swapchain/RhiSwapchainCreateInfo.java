package com.github.slmpc.prismrhi.swapchain;

import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.Objects;

public record RhiSwapchainCreateInfo(
        RhiContext context,
        int width,
        int height,
        int preferredImageCount,
        RhiFormat preferredFormat,
        boolean vsync
) {
    public RhiSwapchainCreateInfo {
        context = Objects.requireNonNull(context, "context");
        width = width <= 0 ? context.width() : width;
        height = height <= 0 ? context.height() : height;
        preferredImageCount = preferredImageCount <= 0 ? 2 : preferredImageCount;
        preferredFormat = preferredFormat == null ? RhiFormat.BGRA8_UNORM : preferredFormat;
    }

    public static Builder builder(RhiContext context) {
        return new Builder(context);
    }

    public static final class Builder {
        private final RhiContext context;
        private int width;
        private int height;
        private int preferredImageCount = 2;
        private RhiFormat preferredFormat = RhiFormat.BGRA8_UNORM;
        private boolean vsync = true;

        private Builder(RhiContext context) {
            this.context = Objects.requireNonNull(context, "context");
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder preferredImageCount(int preferredImageCount) {
            this.preferredImageCount = preferredImageCount;
            return this;
        }

        public Builder preferredFormat(RhiFormat preferredFormat) {
            this.preferredFormat = preferredFormat;
            return this;
        }

        public Builder vsync(boolean vsync) {
            this.vsync = vsync;
            return this;
        }

        public RhiSwapchainCreateInfo build() {
            return new RhiSwapchainCreateInfo(context, width, height, preferredImageCount, preferredFormat, vsync);
        }
    }
}
