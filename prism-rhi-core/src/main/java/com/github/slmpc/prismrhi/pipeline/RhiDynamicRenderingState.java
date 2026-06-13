package com.github.slmpc.prismrhi.pipeline;

import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.ArrayList;
import java.util.List;

public record RhiDynamicRenderingState(
        List<RhiFormat> colorAttachmentFormats,
        RhiFormat depthAttachmentFormat,
        RhiFormat stencilAttachmentFormat,
        int viewMask
) {
    public RhiDynamicRenderingState {
        colorAttachmentFormats = List.copyOf(colorAttachmentFormats == null ? List.of() : colorAttachmentFormats);
        depthAttachmentFormat = depthAttachmentFormat == null ? RhiFormat.UNDEFINED : depthAttachmentFormat;
        stencilAttachmentFormat = stencilAttachmentFormat == null ? RhiFormat.UNDEFINED : stencilAttachmentFormat;
        if (viewMask < 0) {
            throw new IllegalArgumentException("viewMask must not be negative");
        }
    }

    public static RhiDynamicRenderingState color(RhiFormat colorFormat) {
        return new RhiDynamicRenderingState(List.of(colorFormat), RhiFormat.UNDEFINED, RhiFormat.UNDEFINED, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RhiFormat> colorFormats = new ArrayList<>();
        private RhiFormat depthFormat = RhiFormat.UNDEFINED;
        private RhiFormat stencilFormat = RhiFormat.UNDEFINED;
        private int viewMask;

        public Builder color(RhiFormat format) {
            colorFormats.add(format);
            return this;
        }

        public Builder depth(RhiFormat format) {
            depthFormat = format;
            return this;
        }

        public Builder stencil(RhiFormat format) {
            stencilFormat = format;
            return this;
        }

        public Builder viewMask(int viewMask) {
            this.viewMask = viewMask;
            return this;
        }

        public RhiDynamicRenderingState build() {
            return new RhiDynamicRenderingState(colorFormats, depthFormat, stencilFormat, viewMask);
        }
    }
}
