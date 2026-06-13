package com.github.slmpc.prismrhi.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RhiRenderingInfo(
        RhiRect2D renderArea,
        int layerCount,
        int viewMask,
        List<RhiRenderingAttachment> colorAttachments,
        RhiRenderingAttachment depthAttachment,
        RhiRenderingAttachment stencilAttachment
) {
    public RhiRenderingInfo {
        renderArea = Objects.requireNonNull(renderArea, "renderArea");
        if (layerCount < 1) {
            throw new IllegalArgumentException("layerCount must be at least 1");
        }
        if (viewMask < 0) {
            throw new IllegalArgumentException("viewMask must not be negative");
        }
        colorAttachments = List.copyOf(colorAttachments == null ? List.of() : colorAttachments);
    }

    public static Builder builder(RhiRect2D renderArea) {
        return new Builder(renderArea);
    }

    public static final class Builder {
        private final RhiRect2D renderArea;
        private int layerCount = 1;
        private int viewMask;
        private final List<RhiRenderingAttachment> colorAttachments = new ArrayList<>();
        private RhiRenderingAttachment depthAttachment;
        private RhiRenderingAttachment stencilAttachment;

        private Builder(RhiRect2D renderArea) {
            this.renderArea = renderArea;
        }

        public Builder layerCount(int layerCount) {
            this.layerCount = layerCount;
            return this;
        }

        public Builder viewMask(int viewMask) {
            this.viewMask = viewMask;
            return this;
        }

        public Builder color(RhiRenderingAttachment attachment) {
            colorAttachments.add(attachment);
            return this;
        }

        public Builder depth(RhiRenderingAttachment attachment) {
            depthAttachment = attachment;
            return this;
        }

        public Builder stencil(RhiRenderingAttachment attachment) {
            stencilAttachment = attachment;
            return this;
        }

        public RhiRenderingInfo build() {
            return new RhiRenderingInfo(renderArea, layerCount, viewMask, colorAttachments, depthAttachment, stencilAttachment);
        }
    }
}
